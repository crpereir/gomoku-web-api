
insert into dbo.Variants (id, board_dim, opening_rules, playing_rules) values
    (1, 15, 'none', 'freestyle'),
    (2, 19, 'none', 'freestyle');

insert into dbo.SystemInfo (version, created_at) values
    ('0.1.0', '2023-10-8');

insert into dbo.Authors (first_name, surname, email) values
    ('João', 'Bonacho', 'a49437@alunos.isel.pt'),
    ('André', 'Gonçalves', 'a49464@alunos.isel.pt'),
    ('Carolina', 'Pereira', 'a49470@alunos.isel.pt');

insert into dbo.CreatedVersion (system_ver, author_id) values
	('0.1.0', 1),
	('0.1.0', 2),
	('0.1.0', 3);