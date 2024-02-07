import * as React from 'react';
import { error } from '../utils/error/Error';
import { fetchRanking } from '../http/Http';
import { Link } from 'react-router-dom';
import { Siren } from '../utils/infra/Siren';
import { Problem, UnexpectedError } from '../utils/error/Problem';
import { Page, definePage } from '../utils/page/Page';

/**
 * Ranking page states.
 */
export type RankingState =
  | { tag: 'loading' }
  | { tag: 'loadedSuccess'; rankingSiren: Siren; page: Page; error?: Problem }
  | { tag: 'refreshing'; rankingSiren: Siren; page: Page }
  | { tag: 'loadedError'; error: Problem };

/**
 * Actions on ranking page states.
 */
export type RankingAction =
  | { type: 'success'; rankingSiren: Siren; page: Page }
  | { type: 'failure'; error: Problem; rankingSiren?: Siren; page?: Page }
  | { type: 'refresh' }
  | { type: 'retry' };

/**
 * Given a current state of the ranking page and an action produces a new state of the ranking page.
 * @param state
 * @param action
 * @returns the new state of the ranking page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: RankingState, action: RankingAction): RankingState {
  switch (state.tag) {
    case 'loading':
      switch (action.type) {
        case 'success':
          return { tag: 'loadedSuccess', rankingSiren: action.rankingSiren, page: action.page };
        case 'failure':
          return { tag: 'loadedError', error: action.error };
        default:
          error(state, action);
      }
      break;
    case 'loadedSuccess':
      switch (action.type) {
        case 'refresh':
          return { tag: 'refreshing', rankingSiren: state.rankingSiren, page: state.page };
        default:
          error(state, action);
      }
      break;
    case 'refreshing':
      switch (action.type) {
        case 'success':
          return { tag: 'loadedSuccess', rankingSiren: action.rankingSiren, page: action.page };
        case 'failure':
          return { tag: 'loadedSuccess', rankingSiren: action.rankingSiren, page: action.page, error: action.error };
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
function localFetchRanking(dispatch: (action: RankingAction) => void, href?: string) {
  fetchRanking(href).then(res => {
    if (res.tag === 'success') {
      const page: Page = definePage(res.value.links);
      dispatch({ type: 'success', rankingSiren: res.value, page: page });
    } else {
      dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
    }
  });
}

/**
 * Component that describes the ranking page.
 */
export function Ranking() {
  console.log('Ranking Page');
  const [state, dispatch] = React.useReducer(reduce, { tag: 'loading' });

  React.useEffect(() => {
    function localDispatch(action: RankingAction) {
      return dispatch(action);
    }

    localFetchRanking(localDispatch);
  }, []);

  function handleSubmitRefresh() {
    dispatch({ type: 'refresh' });
    localFetchRanking(dispatch);
  }

  function handleChangePage(href: string) {
    dispatch({ type: 'refresh' });
    if (state.tag === 'loading' || state.tag === 'loadedError') return;

    switch (href) {
      case 'first':
        return localFetchRanking(dispatch, state.page.firstHref);
      case 'previous':
        return localFetchRanking(dispatch, state.page.previousHref);
      case 'next':
        return localFetchRanking(dispatch, state.page.nextHref);
      case 'last':
        return localFetchRanking(dispatch, state.page.lastHref);
      default:
        return;
    }
  }

  function handleSubmitRetry() {
    dispatch({ type: 'retry' });
    localFetchRanking(dispatch);
  }

  function isPossibleGoBack() {
    if (state.tag === 'loading' || state.tag === 'loadedError') return;
    return state.page.type === 'middlePage' || state.page.type === 'lastPage';
  }

  function isPossibleGoForward() {
    if (state.tag === 'loading' || state.tag === 'loadedError') return;
    return state.page.type === 'middlePage' || state.page.type === 'firstPage';
  }

  const ranking =
    (state.tag === 'loadedSuccess' || state.tag === 'refreshing') && state.rankingSiren.properties.tag === 'ranking' ? (
      <ol style={{ listStyleType: 'none' }}>
        {state.rankingSiren.properties.value.ranking.map(u => {
          return (
            <li key={u.userId} style={{ marginBottom: '8px' }}>
              {u.rankingPosition}º - <Link to={`/users/${u.userId}`}>{u.username}</Link>
            </li>
          );
        })}
      </ol>
    ) : undefined;

  const refreshButton =
    state.tag === 'loadedSuccess' ? <button onClick={handleSubmitRefresh}>Refresh</button> : undefined;

  const paginationButtons =
    state.tag === 'loadedSuccess' ? (
      <div>
        <button onClick={() => handleChangePage('first')} disabled={!isPossibleGoBack()}>
          {'<<'}
        </button>
        <button onClick={() => handleChangePage('previous')} disabled={!isPossibleGoBack()}>
          {'<'}
        </button>
        <button onClick={() => handleChangePage('next')} disabled={!isPossibleGoForward()}>
          {'>'}
        </button>
        <button onClick={() => handleChangePage('last')} disabled={!isPossibleGoForward()}>
          {'>>'}
        </button>
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
          <Link to="/">Return to home.</Link>
        </p>
      </div>
    ) : undefined;

  return (
    <div>
      <h1>Ranking</h1>
      <hr></hr>
      <ol>
        <p>{refreshButton}</p>
        <p>{ranking}</p>
        <p>{paginationButtons}</p>
      </ol>
      {error}
    </div>
  );
}
