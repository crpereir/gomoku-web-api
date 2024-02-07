import { LogInState, LogInAction } from '../../pages/Login';
import { SignUpState, SignUpAction } from '../../pages/SignUp';
import { RankingState, RankingAction } from '../../pages/Ranking';
import { MeAction, MeState } from '../../pages/Me';
import { GameAction, GameState } from '../../pages/Game';
import { UserStatisticsAction, UserStatisticsState } from '../../pages/UserStatistics';

type State = LogInState | SignUpState | RankingState | UserStatisticsState | MeState | GameState;

type Action = LogInAction | SignUpAction | RankingAction | UserStatisticsAction | MeAction | GameAction;

export function error(state: State, action: Action) {
  console.log(`state: ${state.tag}, action: ${action.type}`);
  throw new Error('Unexpected error.');
}
