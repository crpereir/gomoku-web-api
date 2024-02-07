import { test, expect, Page } from '@playwright/test';
import { delay, getRandomInt, website } from './utils';

const betweenPagesDelay = 1000
const finalPageDelay = 2500

async function canSignUp(page: Page, userDescriminator: number): Promise<void> {
  // then: the page has a link to the 'me' page
  let meLink = page.getByRole('link', { name: 'Play Game', exact: true });
  await expect(meLink).toBeVisible();

  // when: navigating to the 'me' page
  await meLink.click();

  // then: the login form appears
  const signUpLink = page.getByRole('link', { name: 'Sign up.', exact: true });
  await expect(signUpLink).toBeVisible();

  // [delay to show login form]
  await delay(betweenPagesDelay)

  // when: navigating to the 'signup' page
  await signUpLink.click();

  // given: the signup form
  const usernameInput = page.getByLabel('Username ');
  const emailInput = page.getByLabel('Email ');
  const passwordSignUpInput = page.getByLabel('Password ');
  const signUpButton = page.getByRole('button');
  await expect(usernameInput).toBeVisible();
  await expect(emailInput).toBeVisible();
  await expect(passwordSignUpInput).toBeVisible();
  await expect(signUpButton).toBeVisible();

  // [delay to show signup form]
  await delay(betweenPagesDelay)

  // when: providing correct credentials
  await usernameInput.fill(`user-${userDescriminator}`);
  await emailInput.fill(`user-${userDescriminator}@gmail.com`);
  await passwordSignUpInput.fill('Random123');
  await signUpButton.click();

  // then: should go to me
  await expect(page.getByText(`Hello, user-${userDescriminator}`)).toBeVisible();
}

async function canChooseVariantAndStart(page: Page, expectMonitorOrBoard: () => Promise<void>) {
  // given: visible variant and start button
  const variant = 'Playing Rules: FREESTYLE | Opening Rules: NONE | Board Size: 15';
  const variantInput = page.getByLabel(variant);
  const startGameButton = page.getByRole('button', { name: 'Start game' });

  await expect(variantInput).toBeVisible();
  await expect(startGameButton).toBeVisible();

  // when: user chooses variant and clicks start
  await variantInput.click();
  await startGameButton.click();

  // then: user can see that he is either waiting or playing (see canStartGameAndWait and canStartGameAndSeeBoard)
  await expectMonitorOrBoard();
}

async function canStartGameAndWait(page: Page) {
  // when: user can choose variant and click start
  await canChooseVariantAndStart(page, async () => {
    // then: user can see that he is waiting for other player.
    expect(page.getByText('Waiting for other player to join...')).toBeVisible();
  });
}

async function canStartGameAndSeeBoard(page: Page) {
  // when: user can choose variant and click start
  await canChooseVariantAndStart(page, async () => {
    // then: user can see a board of new game.
    expect(page.getByText('Waiting for other player move...')).toBeVisible();
  });
}

async function canFinishGameByWinning(pageUser1: Page, pageUser2: Page) {
  // given: 4 plays from each player
  for (let i: number = 0; i < 4; i++) {
    await canPlay(pageUser1, 0, i);
    await canPlay(pageUser2, 1, i);
  }

  // when: user makes winning play
  await canWin(pageUser1, 0, 4);

  // then: both users can see game over
  await canSeeGameOver(pageUser1);
  await canSeeGameOver(pageUser2);

  // and: user 2 sees that he lost
  await canSeeLoss(pageUser2);
}

async function canPlay(page: Page, row: number, column: number) {
  // when: the user sees it is turn
  await canSeeYourTurn(page);

  // and: clicks on an intersection
  const cellLocation = page.locator(`[id='${row}:${column}']`);
  await expect(cellLocation).toBeVisible();
  await cellLocation.click();

  // then: he should see the piece in the position and see that it is now the opponent's turn
  await canSeeWaitingForOpponent(page);
}

async function canWin(page: Page, row: number, column: number) {
  // when: the user sees it is turn
  await canSeeYourTurn(page);

  // and: clicks on an intersection to make winning game
  const cellLocation = page.locator(`[id='${row}:${column}']`);
  await expect(cellLocation).toBeVisible();
  await cellLocation.click();

  // then: user can see that he won
  await canSeeWin(page);
}

async function canSeeYourTurn(page: Page) {
  await expect(page.getByText('Your Turn')).toBeVisible();
}

async function canSeeWaitingForOpponent(page: Page) {
  await expect(page.getByText('Waiting for other player move...')).toBeVisible();
}

async function canSeeGameOver(page: Page) {
  await expect(page.getByText('Game over.')).toBeVisible();
}

async function canSeeWin(page: Page) {
  await expect(page.getByText('You win!')).toBeVisible();
}

async function canSeeLoss(page: Page) {
  await expect(page.getByText('You lose...')).toBeVisible();
}

async function canSeeRankingAndStatistics(page: Page, userDescriminator: number) {
  // when: user goes to ranking via deep-linking
  await page.goto(website + '/ranking');

  // and: user sees his spot in the ranking
  const userStatsLink = page.getByRole('link', { name: `user-${userDescriminator}` });
  await expect(userStatsLink).toBeVisible();

  // and: user can see the link and clicks
  await userStatsLink.click();

  // then: user can see that he won 1 game
  const gamesWon = page.locator("text='Games Won - 1'");
  await expect(gamesWon).toBeVisible();
}

test('full user experience', async ({ browser }) => {
  // given: user1's browser
  const contextUser1 = await browser.newContext();
  const pageUser1 = await contextUser1.newPage();
  await pageUser1.goto(website);

  // and: user2's browser
  const contextUser2 = await browser.newContext();
  const pageUser2 = await contextUser2.newPage();
  await pageUser2.goto(website);

  // when: user1 tries to sign up
  const user1Id = getRandomInt();
  await canSignUp(pageUser1, user1Id);

  // [delay to show me page for user 1]
  await delay(betweenPagesDelay)

  // then: he can wait for game
  await canStartGameAndWait(pageUser1);

  // when: user2 tries to sign up
  const user2Id = getRandomInt();
  await canSignUp(pageUser2, user2Id);

  // [delay to show me page for user 2]
  await delay(betweenPagesDelay)

  // then: he can start a game and see board
  await canStartGameAndSeeBoard(pageUser2);

  // when: user1 can see the board
  // then: user1 and 2 can play, with user 1 winning
  await canFinishGameByWinning(pageUser1, pageUser2);

  // when: user1 can see ranking and his statistics
  // then: he can see that he got a win.
  await canSeeRankingAndStatistics(pageUser1, user1Id);

  // [delay to show stats of user1.]
  await delay(finalPageDelay);
});
