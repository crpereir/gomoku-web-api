export type Problem = {
  type: string;
  title?: string;
  detail?: string;
  instance?: string;
};

export const UnexpectedError: Problem = {
  type: 'Unexpected Error',
  title: 'Something went wrong. Please try again later.',
};

export const NoConnectionError: Problem = {
  type: 'No Connection Error',
  title: 'Something went wrong. Please try again later.',
};

export const NotAuthorizedError: Problem = {
  type: 'Not Authorized Error',
  title: 'You do not have permissions to access the resource.',
};

export const GAME_NOT_FOUND_TYPE =
  'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/problem/gameNotFound';
