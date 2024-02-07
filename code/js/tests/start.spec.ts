import { test, expect, Page } from '@playwright/test';
import { getRandomInt, website } from './utils';

async function canSignUp(page: Page, userDescriminator: number): Promise<void> {
  // then: the page has a link to the 'me' page
  let meLink = page.getByRole('link', { name: 'Play Game', exact: true });
  await expect(meLink).toBeVisible();

  // when: navigating to the 'me' page
  await meLink.click();

  // then: the login form appears
  const signUpLink = page.getByRole('link', { name: 'Sign up.', exact: true });
  await expect(signUpLink).toBeVisible();

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

  // when: providing correct credentials
  await usernameInput.fill(`user-${userDescriminator}`);
  await emailInput.fill(`user-${userDescriminator}@gmail.com`);
  await passwordSignUpInput.fill('Random123');
  await signUpButton.click();

  // then: should go to me
  await expect(page.getByText(`Hello, user-${userDescriminator}`)).toBeVisible();
}
async function canStartGameAndGiveUp(page: Page, userDescriminator: number) {
  // given: visible variant and start button
  const variant = 'Playing Rules: FREESTYLE | Opening Rules: NONE | Board Size: 19';
  const variantInput = page.getByLabel(variant);
  const startGameButton = page.getByRole('button', { name: 'Start game' });

  await expect(variantInput).toBeVisible();
  await expect(startGameButton).toBeVisible();

  // when: user chooses variant and clicks start
  await variantInput.click();
  await startGameButton.click();

  // then: user sees that he is waiting
  await expect(page.getByText('Waiting for other player to join...')).toBeVisible();

  // when: user sees give up button and wants to give up
  const giveUpButton = page.getByRole('button', { name: 'Give up?' });
  await expect(giveUpButton).toBeVisible();

  // and: clicks the button
  await giveUpButton.click();

  // then: user goes back to me page
  await expect(page.getByText(`Hello, user-${userDescriminator}`)).toBeVisible();
}

test('can start game and give up on waiting for other player', async ({ browser }) => {
  // given: user 1's browser
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto(website);

  // when: he can sign up
  const user1Id = getRandomInt();
  await canSignUp(page, user1Id);

  // then: he can start game and give up on waiting
  await canStartGameAndGiveUp(page, user1Id);
});
