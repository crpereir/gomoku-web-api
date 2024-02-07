import * as React from 'react';
import { Link, Navigate } from 'react-router-dom';
import { error } from '../utils/error/Error';
import { fetchVariants, logout, me, startGame } from '../http/Http';
import { Siren, SirenAction, SirenLink, getAction, getLink } from '../utils/infra/Siren';
import { Problem, UnexpectedError } from '../utils/error/Problem';
import { Variants } from '../domain/Variants';

/**
 * User home page states.
 */
export type MeState =
  | { tag: 'loading' }
  | { tag: 'loadedSuccess'; meSiren: Siren; variants: Variants; error?: Problem; variant?: number }
  | { tag: 'loadedError'; error: Problem }
  | { tag: 'starting'; meSiren: Siren; variants: Variants }
  | { tag: 'redirect'; selfLink: SirenLink; variant?: number }
  | { tag: 'loggingOut'; meSiren: Siren; variants: Variants }
  | { tag: 'loggedOut' };

/**
 * Actions on user home page states.
 */
export type MeAction =
  | { type: 'success'; meSiren?: Siren; variants?: Variants; selfLink?: SirenLink; variant?: number }
  | { type: 'failure'; error: Problem }
  | { type: 'retry' }
  | { type: 'start'; meSiren: Siren; variants: Variants }
  | { type: 'logout' }
  | { type: 'variantChosen'; variant: number };

/**
 * Given a current state of the user home page and an action produces a new state of the user home page.
 * @param state
 * @param action
 * @returns the new state of the user home page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: MeState, action: MeAction): MeState {
  switch (state.tag) {
    case 'loading':
      switch (action.type) {
        case 'success':
          return { tag: 'loadedSuccess', meSiren: action.meSiren, variants: action.variants };
        case 'failure':
          return { tag: 'loadedError', error: action.error };
        default:
          error(state, action);
      }
      break;
    case 'loadedSuccess':
      switch (action.type) {
        case 'variantChosen':
          return { tag: 'loadedSuccess', meSiren: state.meSiren, variants: state.variants, variant: action.variant };
        case 'start':
          return { tag: 'starting', meSiren: action.meSiren, variants: action.variants };
        case 'logout':
          return { tag: 'loggingOut', meSiren: state.meSiren, variants: state.variants };
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
      break;
    case 'starting':
      switch (action.type) {
        case 'success':
          return { tag: 'redirect', selfLink: action.selfLink, variant: action.variant };
        case 'failure':
          return { tag: 'loadedSuccess', meSiren: state.meSiren, variants: state.variants, error: action.error };
        default:
          error(state, action);
      }
      break;
    case 'loggingOut':
      switch (action.type) {
        case 'success':
          return { tag: 'loggedOut' };
        case 'failure':
          return { tag: 'loadedSuccess', meSiren: state.meSiren, variants: state.variants, error: action.error };
      }
      break;
    default:
      error(state, action);
  }
}

/**
 * Get information from WebApi and transitions to next state with it.
 */
function localFetchMe(dispatch: (action: MeAction) => void) {
  me().then(res => {
    if (res.tag === 'success') {
      const linkVariants = getLink(
        res.value.links,
        'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/variants'
      );
      fetchVariants(linkVariants).then(resVariants => {
        if (resVariants.tag === 'success' && resVariants.value.properties.tag === 'variants') {
          dispatch({ type: 'success', meSiren: res.value, variants: resVariants.value.properties.value });
        } else {
          dispatch({ type: 'failure', error: UnexpectedError });
        }
      });
    } else {
      dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
    }
  });
}

/**
 * Component that describes the user home page.
 */
export function Me() {
  console.log('Me Page');
  const [state, dispatch] = React.useReducer(reduce, { tag: 'loading' });

  React.useEffect(() => {
    function localDispatch(action: MeAction) {
      return dispatch(action);
    }

    localFetchMe(localDispatch);
  }, []);

  if (state.tag === 'redirect') {
    const id = state.selfLink.href.split('/')[3];
    return <Navigate to={`/games/${id}`} />;
  }

  if (state.tag === 'loggedOut') {
    return <Navigate to="/" />;
  }

  function handleSubmitRetry() {
    dispatch({ type: 'retry' });
    localFetchMe(dispatch);
  }

  function handleSubmitStart(ev: React.FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    if (state.tag !== 'loadedSuccess') return;

    const variantId = state.variant ? state.variant : 1;

    dispatch({ type: 'start', meSiren: state.meSiren, variants: state.variants });

    const startAction: SirenAction = getAction(state.meSiren.actions, 'start-game');

    startGame(startAction, variantId).then(res => {
      if (res.tag === 'success') {
        dispatch({ type: 'success', selfLink: getLink(res.value.links, 'self'), variant: variantId });
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    });
  }

  function handleLogOut() {
    if (state.tag !== 'loadedSuccess') return;
    dispatch({ type: 'logout' });

    const logoutAction: SirenAction = getAction(state.meSiren.actions, 'logout');

    logout(logoutAction).then(res => {
      if (res.tag === 'success') {
        dispatch({ type: 'success' });
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    });
  }

  function onVariantChosen(ev: React.ChangeEvent<HTMLInputElement>) {
    dispatch({
      type: 'variantChosen',
      variant: parseInt(ev.target.value),
    });
  }

  const welcome =
    (state.tag === 'loadedSuccess' || state.tag === 'starting') && state.meSiren.properties.tag === 'user' ? (
      <div>Hello, {state.meSiren.properties.value.username}</div>
    ) : undefined;

  const variants =
    state.tag === 'loadedSuccess' || state.tag === 'starting' ? (
      <div>
        <h3>Select a variant to start a game</h3>
        <form onSubmit={handleSubmitStart}>
          {state.variants.variants.map(v => {
            return (
              <div key={v.variantId} style={{ marginBottom: '10px' }}>
                <input
                  type="radio"
                  name="variant"
                  id={`variant-${v.variantId}`}
                  value={`${v.variantId}`}
                  onChange={onVariantChosen}
                  disabled={state.tag !== 'loadedSuccess'}
                />
                <label htmlFor={`variant-${v.variantId}`}>
                  Playing Rules: {v.playingRules} | Opening Rules: {v.openingRules} | Board Size: {v.boardDim}
                </label>
              </div>
            );
          })}
          <button type="submit" disabled={state.tag !== 'loadedSuccess'}>Start game </button>
        </form>
      </div>
    ) : undefined;

  const logoutButton = state.tag === 'loadedSuccess' ? <button onClick={handleLogOut}>Logout</button> : undefined;

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

  const startError =
    state.tag === 'loadedSuccess' && state.error !== undefined ? (
      <div>
        <h4>{state.error.title}</h4>
      </div>
    ) : undefined;

  return (
    <div>
      <h1>{welcome}</h1>
      <p>{logoutButton}</p>
      <hr></hr>
      {error}
      {variants}
      {startError}
    </div>
  );
}
