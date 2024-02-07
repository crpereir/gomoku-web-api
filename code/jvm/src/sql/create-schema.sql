create schema dbo;

create table dbo.Users(
    id int generated always as identity primary key,
    username varchar(64) unique not null,
    email varchar(64) unique not null,
    password_validation varchar(256) not null,
    games_won int not null default 0,
    games_lost int not null default 0,
    games_tied int not null default 0
);

create table dbo.Tokens(
    token_validation VARCHAR(256) primary key,
    user_id int references dbo.Users(id),
    created_at bigint not null,
    last_used_at bigint not null
);

create table dbo.SystemInfo(
    version varchar(8) unique not null,
    created_at date not null
);

create table dbo.Authors(
    id int generated always as identity primary key,
    email varchar(64) unique not null,
    first_name varchar(64) not null,
    surname varchar(64) not null
);

create table dbo.CreatedVersion(
    system_ver varchar(64) references dbo.SystemInfo(version),
    author_id int references dbo.Authors(id)
);

create table dbo.Variants(
    id int not null primary key,
    board_dim int not null,
    opening_rules varchar(64) not null,
    playing_rules varchar(64) not null
);

create table dbo.Games(
    id UUID not null primary key,
    board jsonb not null,
    state varchar(64) check (state like 'NEXT_TURN_B' or state like 'NEXT_TURN_W' or state like 'WINNER_B' or state like 'WINNER_W' or state like 'DRAW'),
    created_at bigint not null,
    updated_at bigint check (updated_at >= created_at),
    deadline bigint not null check (deadline > created_at),
    created_player int not null references dbo.Users(id),
    joined_player int references dbo.Users(id),
    variant_id int not null references dbo.Variants(id)
);

create table dbo.Waiting_Lobby(
    id UUID not null primary key,
    created_user_id int not null references dbo.Users(id),
    created_at bigint not null,
    deadline bigint not null,
    variant_id int references dbo.Variants(id),
    game_id UUID not null
);

create or replace function automatic_matchmaking() returns trigger as
$$
declare
    otherWaitingLobby record;
begin
    select wl.game_id, wl.created_user_id into otherWaitingLobby
        from dbo.Waiting_Lobby wl where wl.variant_id = new.variant_id order by wl.created_at
        limit 1;
    if otherWaitingLobby.game_id is not null then
        insert into
            dbo.Games(
                id, board, state, created_at, updated_at, deadline, created_player, joined_player, variant_id
            )
        values (
            otherWaitingLobby.game_id,
            '{"pieces": []}'::jsonb,
            'NEXT_TURN_B',
            -- Note: Instants are the ones registered in the most recent record of waiting lobby.
            -- As this trigger mainly runs when there are multiple people starting at once, so times
            -- are vaguely similar enough to not be noticeable.
            new.created_at, new.created_at, new.created_at + 360,
            otherWaitingLobby.created_user_id, new.created_user_id, new.variant_id
        );
        delete from dbo.Waiting_Lobby where game_id == otherWaitingLobby.game_id;
        return null;
    else
        return new;
    end if;
end;
$$ language plpgsql;

create trigger automatic_matchmaking_trigger
before insert on dbo.Waiting_Lobby
for each row
    execute function automatic_matchmaking();

create view dbo.Ranking_View as
select
    ROW_NUMBER() over (
        order by
            ( case
                  when (games_won + games_lost + games_tied) = 0 then 0::numeric(5, 2)
                  else ( (games_won + 0.5 * games_lost) / (games_won + games_lost + games_tied) )::numeric(5, 2)
              end
            ) DESC,
            games_won DESC,
            games_lost ASC,
            games_tied ASC
    ) as ranking_position,
    id as user_id
from dbo.Users;

create or replace function update_statistics() returns trigger
language plpgsql
as
$$
begin
    if new.state = 'WINNER_B' then
        update dbo.Users set games_won = games_won + 1 where id = new.created_player;
        update dbo.Users set games_lost = games_lost + 1 where id = new.joined_player;
    elsif new.state = 'WINNER_W' then
        update dbo.Users set games_lost = games_lost + 1 where id = new.created_player;
        update dbo.Users set games_won = games_won + 1 where id = new.joined_player;
    elsif new.state = 'DRAW' then
        update dbo.Users set games_tied = games_tied + 1 where id in (new.created_player, new.joined_player);
    end if;
    return new;
end;
$$;

create trigger update_statistics_trigger
after update of state on dbo.Games
for each row
when (new.state <> old.state and new.state in ('WINNER_B', 'WINNER_W', 'DRAW'))
    execute function update_statistics();