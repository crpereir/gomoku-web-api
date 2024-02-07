export type UserAuthenticated = {
  userId: number;
  username: string;
};

export function getCookie(name: string): string | undefined {
  const value = document.cookie
    .split('; ')
    .find(cookie => cookie.startsWith(`${name}=`))
    ?.split('=')[1];

  return value;
}

export function getUserAuthenticated(): UserAuthenticated {
  const info = getCookie('login').split('-');
  const userAuthenticated: UserAuthenticated = {
    userId: parseInt(info[0]),
    username: info[1],
  };
  return userAuthenticated;
}
