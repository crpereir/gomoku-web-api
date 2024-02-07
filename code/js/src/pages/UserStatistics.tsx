import * as React from 'react';
import { error } from '../utils/error/Error';
import { Problem, UnexpectedError } from '../utils/error/Problem';
import { Siren } from '../utils/infra/Siren';
import { fetchUserStatistics } from '../http/Http';
import { Link, useParams } from 'react-router-dom';

/**
 * User statistics page states.
 */
export type UserStatisticsState =
  | { tag: 'loading' }
  | { tag: 'loadedSuccess'; userStatisticsSiren: Siren }
  | { tag: 'loadedError'; error: Problem };

/**
 * Actions on user statistics page states.
 */
export type UserStatisticsAction =
  | { type: 'success'; userStatisticsSiren: Siren }
  | { type: 'failure'; error: Problem }
  | { type: 'retry' };

/**
 * Given a current state of the user statistics page and an action produces a new state of the user statistics page.
 * @param state
 * @param action
 * @returns the new state of the user statistics page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: UserStatisticsState, action: UserStatisticsAction): UserStatisticsState {
  switch (state.tag) {
    case 'loading':
      switch (action.type) {
        case 'success':
          return { tag: 'loadedSuccess', userStatisticsSiren: action.userStatisticsSiren };
        case 'failure':
          return { tag: 'loadedError', error: action.error };
        default:
          error(state, action);
      }
      break;
    case 'loadedSuccess':
      switch (action.type) {
        default:
          error(state, action);
      }
      break;
    case 'loadedError':
      switch (action.type) {
        case 'retry':
          return { tag: 'loading' };
        default:
          error(state, action);
      }
  }
}

/**
 * Get information from WebApi and transitions to next state with it.
 */
function localFetchUserStatistics(uid: string, dispatch: (action: UserStatisticsAction) => void) {
  fetchUserStatistics(uid).then(res => {
    if (res.tag === 'success') {
      dispatch({ type: 'success', userStatisticsSiren: res.value });
    } else {
      dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
    }
  });
}

/**
 * Component that describes the user statistics page.
 */
export function UserStatistics() {
  console.log('UserStatistics Page');
  const [state, dispatch] = React.useReducer(reduce, { tag: 'loading' });
  const { uid } = useParams();

  React.useEffect(() => {
    function localDispatch(action: UserStatisticsAction) {
      return dispatch(action);
    }

    localFetchUserStatistics(uid, localDispatch);
  }, [uid]);

  function handleSubmitRetry() {
    dispatch({ type: 'retry' });
    localFetchUserStatistics(uid, dispatch);
  }

  const userStatistics =
    state.tag === 'loadedSuccess' && state.userStatisticsSiren.properties.tag === 'userStatistics' ? (
      <div>
        <p>Username - {state.userStatisticsSiren.properties.value.username}</p>
        <p>Ranking Position - {state.userStatisticsSiren.properties.value.rankingPosition}</p>
        <p>Games Played - {state.userStatisticsSiren.properties.value.gamesPlayed}</p>
        <p>Games Won - {state.userStatisticsSiren.properties.value.gamesWon}</p>
        <p>Games Lost - {state.userStatisticsSiren.properties.value.gamesLost}</p>
        <p>Games Tied - {state.userStatisticsSiren.properties.value.gamesTied}</p>
      </div>
    ) : undefined;

  const error =
    state.tag === 'loadedError' ? (
      <div>
        <p>
          <h4>{state.error && state.error.title}</h4>
        </p>
        <p>
          <button onClick={handleSubmitRetry}>Retry</button>
        </p>
        <p>
          <Link to="/">Return to home</Link>
        </p>
      </div>
    ) : undefined;

  return (
    <div>
      <h1>User Statistics</h1>
      <hr></hr>
      <ol>{userStatistics}</ol>
      {error}
    </div>
  );
}
