export const website = 'http://localhost:8080';

export function delay(delayInMs: number) {
  return new Promise(resolve => {
    setTimeout(() => resolve(undefined), delayInMs);
  });
}

export function getRandomInt() {
  return Math.floor(Math.random() * 999999999);
}
