import * as React from 'react';
import { Link, Navigate, useParams } from 'react-router-dom';
import { error } from '../utils/error/Error';
import { play, fetchGame, fetchMonitor, giveUp, deleteMonitor, fetchVariant } from '../http/Http';
import { Siren, SirenAction, SirenLink, getAction, getLink } from '../utils/infra/Siren';
import { GAME_NOT_FOUND_TYPE, Problem, UnexpectedError } from '../utils/error/Problem';
import { getUserAuthenticated } from '../utils/auth/Authn';
import { Board } from '../components/Board';
import { Spinner } from '../components/Spinner';
import { Game, GameCurrentState } from '../domain/Game';

const DEFAULT_POLLING_MONITOR_VALUE = 10000;
const DEFAULT_POLLING_GAME_VALUE = 3000;

/**
 * Game page states.
 */
export type GameState =
  | { tag: 'loading' }
  | { tag: 'monitor'; pollingLink: SirenLink; giveUpAction: SirenAction; error?: Problem }
  | { tag: 'playingGame'; gameSiren: Siren; error?: Problem; boardDim: number }
  | { tag: 'waitingGame'; gameSiren: Siren; error?: Problem; boardDim: number }
  | { tag: 'gameOver'; game: Game; boardDim: number }
  | { tag: 'error'; error: Problem }
  | { tag: 'playProcessing'; gameSiren: Siren; boardDim: number }
  | { tag: 'giveUpProcessing'; gameSiren?: Siren; giveUpAction?: SirenAction; boardDim?: number }
  | { tag: 'redirect' };

/**
 * Actions on game page states.
 */
export type GameAction =
  | { type: 'waitStart'; pollingLink: SirenLink; giveUpAction: SirenAction }
  | { type: 'myTurn'; gameSiren: Siren; boardDim?: number }
  | { type: 'otherTurn'; gameSiren: Siren; boardDim: number }
  | { type: 'end'; game: Game; boardDim?: number }
  | { type: 'play'; gameSiren: Siren; boardDim: number }
  | { type: 'giveUp'; gameSiren?: Siren; giveUpAction?: SirenAction; boardDim?: number }
  | { type: 'success' }
  | { type: 'failure'; error: Problem }
  | { type: 'retry' };

/**
 * Given a current state of the game page and an action produces a new state of the game page.
 * @param state
 * @param action
 * @returns the new state of the game page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: GameState, action: GameAction): GameState {
  switch (state.tag) {
    case 'loading':
      switch (action.type) {
        case 'waitStart':
          return { tag: 'monitor', pollingLink: action.pollingLink, giveUpAction: action.giveUpAction };
        case 'myTurn':
          return { tag: 'playingGame', gameSiren: action.gameSiren, boardDim: action.boardDim };
        case 'otherTurn':
          return { tag: 'waitingGame', gameSiren: action.gameSiren, boardDim: action.boardDim };
        case 'end':
          return { tag: 'gameOver', game: action.game, boardDim: action.boardDim };
        case 'failure':
          return { tag: 'error', error: action.error };
        default:
          error(state, action);
      }
      break;
    case 'monitor':
      switch (action.type) {
        case 'waitStart':
          return { tag: 'monitor', pollingLink: state.pollingLink, giveUpAction: state.giveUpAction };
        case 'myTurn':
          return { tag: 'playingGame', gameSiren: action.gameSiren, boardDim: action.boardDim };
        case 'giveUp':
          return { tag: 'giveUpProcessing', gameSiren: action.gameSiren };
        case 'failure':
          return {
            tag: 'monitor',
            pollingLink: state.pollingLink,
            giveUpAction: state.giveUpAction,
            error: action.error,
          };
        default:
          error(state, action);
      }
      break;
    case 'playingGame':
      switch (action.type) {
        case 'play':
          return { tag: 'playProcessing', gameSiren: action.gameSiren, boardDim: action.boardDim };
        case 'giveUp':
          return { tag: 'giveUpProcessing', gameSiren: action.gameSiren };
        default:
          error(state, action);
      }
      break;
    case 'waitingGame':
      switch (action.type) {
        case 'myTurn':
          return { tag: 'playingGame', gameSiren: action.gameSiren, boardDim: state.boardDim };
        case 'otherTurn':
          return { tag: 'waitingGame', gameSiren: state.gameSiren, boardDim: action.boardDim };
        case 'end':
          return { tag: 'gameOver', game: action.game, boardDim: action.boardDim ? action.boardDim : state.boardDim };
        case 'giveUp':
          return { tag: 'giveUpProcessing', gameSiren: action.gameSiren };
        case 'failure':
          return { tag: 'waitingGame', gameSiren: state.gameSiren, error: action.error, boardDim: state.boardDim };
        default:
          error(state, action);
      }
      break;
    case 'gameOver':
      error(state, action);
      break;
    case 'error':
      switch (action.type) {
        case 'retry':
          return { tag: 'loading' };
        default:
          error(state, action);
      }
      break;
    case 'playProcessing':
      switch (action.type) {
        case 'otherTurn':
          return { tag: 'waitingGame', gameSiren: action.gameSiren, boardDim: action.boardDim };
        case 'end':
          return { tag: 'gameOver', game: action.game, boardDim: action.boardDim };
        case 'failure':
          return { tag: 'playingGame', gameSiren: state.gameSiren, error: action.error, boardDim: state.boardDim };
        default:
          error(state, action);
      }
      break;
    case 'giveUpProcessing':
      switch (action.type) {
        case 'success':
          return { tag: 'redirect' };
        case 'failure':
          return { tag: 'redirect' };
        default:
          error(state, action);
      }
      break;
    case 'redirect':
      error(state, action);
  }
}

function isMyTurnToPlay(userId: number, state: GameCurrentState, playerB: number, playerW: number): boolean {
  switch (userId) {
    case playerB:
      return state === 'NEXT_TURN_B';
    case playerW:
      return state === 'NEXT_TURN_W';
    default:
      new Error('Unexpected error.');
  }
}

function isGameOver(state: GameCurrentState): boolean {
  return state === 'WINNER_B' || state === 'WINNER_W' || state === 'DRAW';
}

function isMyWin(userId: number, game: Game): boolean {
  switch (game.gameState) {
    case 'WINNER_B':
      return userId === game.playerB;
    case 'WINNER_W':
      return userId === game.playerW;
    default:
      return false;
  }
}

function getGameOverMessage(userId: number, game: Game): string {
  switch (game.gameState) {
    case 'DRAW':
      return 'Game ended in draw.';
    case 'WINNER_B':
    case 'WINNER_W':
      return isMyWin(userId, game) ? 'You win!' : 'You lose...';
    default:
      return 'Not game over.';
  }
}

function pollingMonitor(userId: number, gid: string, dispatch: (action: GameAction) => void) {
  fetchMonitor(gid).then(res => {
    if (res.tag === 'success') {
      if (res.value.properties.tag === 'monitor' && res.value.properties.value.status === 'OTHER_PLAYER_JOINED') {
        pollingGame(userId, gid, dispatch, true);
      } else {
        dispatch({
          type: 'waitStart',
          pollingLink: getLink(res.value.links, 'self'),
          giveUpAction: getAction(res.value.actions, 'cancel'),
        });
        setTimeout(
          () => {
            pollingMonitor(userId, gid, dispatch);
          },
          res.value.properties.tag === 'monitor'
            ? parseInt(res.value.properties.value.askAgainIn.slice(0, -1)) * 1000
            : DEFAULT_POLLING_MONITOR_VALUE
        );
      }
    } else {
      dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
    }
  });
}

function pollingGame(userId: number, gid: string, dispatch: (action: GameAction) => void, needsBoardDim: boolean) {
  fetchGame(gid).then(res => {
    if (res.tag === 'success') {
      if (res.value.properties.tag === 'game') {
        if (needsBoardDim) {
          const link = getLink(
            res.value.links,
            'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/variant'
          );
          fetchVariant(link).then(variantSiren => {
            if (variantSiren.tag === 'success' && variantSiren.value.properties.tag === 'variant') {
              checkGameState(userId, gid, res.value, dispatch, variantSiren.value.properties.value.boardDim);
            } else {
              dispatch({ type: 'failure', error: UnexpectedError });
            }
          });
        } else {
          checkGameState(userId, gid, res.value, dispatch);
        }
      }
    } else {
      dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
    }
  });
}

function checkGameState(
  userId: number,
  gid: string,
  sirenGame: Siren,
  dispatch: (action: GameAction) => void,
  boardDim?: number
) {
  if (sirenGame.properties.tag !== 'game') return;
  const game = sirenGame.properties.value;

  if (isGameOver(game.gameState)) {
    dispatch({ type: 'end', game: game });
  } else if (isMyTurnToPlay(userId, game.gameState, game.playerB, game.playerW)) {
    if (boardDim) {
      dispatch({
        type: 'myTurn',
        gameSiren: sirenGame,
        boardDim: boardDim,
      });
    } else {
      dispatch({
        type: 'myTurn',
        gameSiren: sirenGame,
      });
    }
  } else {
    setTimeout(() => {
      if (boardDim) {
        pollingGame(userId, gid, dispatch, true);
      } else {
        pollingGame(userId, gid, dispatch, false);
      }
    }, DEFAULT_POLLING_GAME_VALUE);
  }
}

function loading(gid: string, dispatch: (action: GameAction) => void) {
  fetchGame(gid).then(res => {
    const userId = getUserAuthenticated().userId;
    if (res.tag === 'success') {
      if (res.value.properties.tag === 'game') {
        const game = res.value.properties.value;
        const link = getLink(
          res.value.links,
          'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/variant'
        );
        fetchVariant(link).then(variantSiren => {
          if (variantSiren.tag === 'success' && variantSiren.value.properties.tag === 'variant') {
            if (isMyTurnToPlay(userId, game.gameState, game.playerB, game.playerW)) {
              dispatch({
                type: 'myTurn',
                gameSiren: res.value,
                boardDim: variantSiren.value.properties.value.boardDim,
              });
            } else {
              dispatch({
                type: 'otherTurn',
                gameSiren: res.value,
                boardDim: variantSiren.value.properties.value.boardDim,
              });
              pollingGame(userId, gid, dispatch, false);
            }
          }
        });
      }
    } else {
      if (res.value.type === GAME_NOT_FOUND_TYPE) {
        pollingMonitor(userId, gid, dispatch);
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    }
  });
}

/**
 * Component that describes the game page.
 */
export function Game() {
  console.log('Game Page');
  const [state, dispatch] = React.useReducer(reduce, { tag: 'loading' });

  const { gid } = useParams();
  const currentUser = getUserAuthenticated();

  React.useEffect(() => {
    function localDispatch(action: GameAction) {
      dispatch(action);
    }

    loading(gid, localDispatch);
  }, [gid]);

  if (state.tag === 'redirect') {
    return <Navigate to={'/me'} />;
  }

  function localDispatch(action: GameAction) {
    dispatch(action);
  }

  function handlePlay(row: number, column: number) {
    if (state.tag != 'playingGame') return;
    dispatch({ type: 'play', gameSiren: state.gameSiren, boardDim: state.boardDim });
    const playAction = getAction(state.gameSiren.actions, 'play');
    play(playAction, gid, row, column).then(res => {
      if (res.tag === 'success') {
        if (res.value.properties.tag === 'game' && isGameOver(res.value.properties.value.gameState)) {
          dispatch({ type: 'end', game: res.value.properties.value, boardDim: state.boardDim });
          return;
        }
        dispatch({ type: 'otherTurn', gameSiren: res.value, boardDim: state.boardDim });
        pollingGame(currentUser.userId, gid, localDispatch, false);
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    });
  }

  function handleGiveUp() {
    if (state.tag === 'playingGame') {
      dispatch({ type: 'giveUp', gameSiren: state.gameSiren });
      const giveUpAction = getAction(state.gameSiren.actions, 'give-up');
      giveUp(giveUpAction, gid).then(res => {
        if (res.tag === 'success') {
          dispatch({ type: 'success' });
        } else {
          dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
        }
      });
    } else if (state.tag === 'monitor') {
      dispatch({ type: 'giveUp' });
      deleteMonitor(state.giveUpAction, gid).then(res => {
        if (res.tag === 'success') {
          dispatch({ type: 'success' });
        } else {
          dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
        }
      });
    } else return;
  }

  function handleSubmitRetry() {
    dispatch({ type: 'retry' });
    loading(gid, dispatch);
  }

  const monitor =
    state.tag === 'monitor' ? (
      <div>
        <h4>Waiting for other player to join...</h4>
        <Spinner></Spinner>
      </div>
    ) : undefined;

  const playingGame =
    state.tag === 'playingGame' && state.gameSiren.properties.tag === 'game' ? (
      <div>
        <h4>Your Turn</h4>
        <Board
          pieces={state.gameSiren.properties.value.pieces}
          boardSize={state.boardDim}
          onIntersectionClick={handlePlay}
        />
      </div>
    ) : undefined;

  const waitingGameOrPlayProcessing =
    state.tag === 'waitingGame' || state.tag === 'playProcessing' ? (
      <div>
        <h4>Waiting for other player move...</h4>
        <Board
          pieces={state.gameSiren.properties.tag === 'game' ? state.gameSiren.properties.value.pieces : undefined}
          boardSize={state.boardDim}
        />
      </div>
    ) : undefined;

  const gameOver =
    state.tag === 'gameOver' ? (
      <div>
        <h4>Game over.</h4>
        <Board pieces={state.game.pieces} boardSize={state.boardDim} />
        <h1>{getGameOverMessage(getUserAuthenticated().userId, state.game)}</h1>
        <p>
          <Link to="/me">Back Home</Link>
        </p>
      </div>
    ) : undefined;

  function canGiveUp(): boolean {
    return state.tag === 'playingGame' || state.tag === 'monitor';
  }
  const giveUpButton = (
    <button onClick={handleGiveUp} disabled={!canGiveUp()}>
      Give up?
    </button>
  );

  const errorMessage =
    state.tag === 'playingGame' || state.tag === 'waitingGame' || state.tag === 'monitor' ? (
      <div>
        <h4>{state.error && state.error.title}</h4>
      </div>
    ) : undefined;

  const errorPage =
    state.tag === 'error' ? (
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
      <h1>Game</h1>
      <hr></hr>
      {errorPage}
      {monitor}
      {playingGame}
      {waitingGameOrPlayProcessing}
      {gameOver}
      {errorMessage}
      {giveUpButton}
    </div>
  );
}
