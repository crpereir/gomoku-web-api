import { test, expect, Page } from '@playwright/test';
import { getRandomInt, website } from './utils';

async function notValidSignUp(
  page: Page,
  username: string,
  email: string,
  password: string,
  error: string
): Promise<void> {
  // then: the signup form appears
  const usernameInput = page.getByLabel('Username ');
  const emailInput = page.getByLabel('Email ');
  const passwordSignUpInput = page.getByLabel('Password ');
  const signUpButton = page.getByRole('button');
  await expect(usernameInput).toBeVisible();
  await expect(emailInput).toBeVisible();
  await expect(passwordSignUpInput).toBeVisible();
  await expect(signUpButton).toBeVisible();

  // when: providing invalid credentials
  await usernameInput.fill(username);
  await emailInput.fill(email);
  await passwordSignUpInput.fill(password);
  await signUpButton.click();

  // then: the error message appears
  await expect(page.getByText(error)).toBeVisible();

  // and: nothing is preserved
  await expect(usernameInput).toHaveValue('');
  await expect(emailInput).toHaveValue('');
  await expect(passwordSignUpInput).toHaveValue('');
}

async function notValidLogIn(page: Page, identity: string, password: string, error: string): Promise<void> {
  // then: the signup form appears
  const identityInput = page.getByLabel('Username or Email ');
  const passwordLoginInput = page.getByLabel('Password ');
  const loginButton = page.getByRole('button');
  await expect(identityInput).toBeVisible();
  await expect(passwordLoginInput).toBeVisible();
  await expect(loginButton).toBeVisible();

  // when: providing invalid credentials
  await identityInput.fill(identity);
  await passwordLoginInput.fill(password);
  await loginButton.click();

  // then: the error message appears
  await expect(page.getByText(error)).toBeVisible();

  // and: nothing is preserved
  await expect(identityInput).toHaveValue(identity);
  await expect(passwordLoginInput).toHaveValue('');
}

async function canSignUp(page: Page, userDescriminator: number): Promise<void> {
  // then: the page has a link to the 'me' page
  let meLink = page.getByRole('link', { name: 'Play Game', exact: true });
  await expect(meLink).toBeVisible();

  // when: navigating to the 'me' page
  await meLink.click();

  // then: the login form appears
  const identityInput = page.getByLabel('Username or Email ');
  const passwordLoginInput = page.getByLabel('Password ');
  const loginButton = page.getByRole('button');
  const signUpLink = page.getByRole('link', { name: 'Sign up.', exact: true });
  await expect(identityInput).toBeVisible();
  await expect(passwordLoginInput).toBeVisible();
  await expect(loginButton).toBeVisible();
  await expect(signUpLink).toBeVisible();

  // when: navigating to the 'signup' page
  await signUpLink.click();

  // then: the signup form appears
  // when: inputting invalid info
  // then: error when not valid
  await notValidSignUp(page, 'alice', 'alice@gmail.com', '123', 'Password not secure');
  await notValidSignUp(page, 'alice', 'alice', 'Random123', 'Email not valid');

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

async function canLogOut(page: Page) {
  // when: trying to logout
  const logoutButton = page.getByRole('button', { name: 'Logout' });
  await expect(logoutButton).toBeVisible();
  await logoutButton.click();

  // then: should go back to home and me link appears
  const meLink = page.getByRole('link', { name: 'Play Game', exact: true });
  await expect(meLink).toBeVisible();
}
async function canLogin(page: Page, userDescriminator: number) {
  const meLink = page.getByRole('link', { name: 'Play Game', exact: true });
  await expect(meLink).toBeVisible();

  // when: clicking link
  await meLink.click();

  // then: the login form appears
  const identityInput = page.getByLabel('Username or Email ');
  const passwordLoginInput = page.getByLabel('Password ');
  const loginButton = page.getByRole('button');
  const signUpLink = page.getByRole('link', { name: 'Sign up.', exact: true });
  await expect(identityInput).toBeVisible();
  await expect(passwordLoginInput).toBeVisible();
  await expect(loginButton).toBeVisible();
  await expect(signUpLink).toBeVisible();

  // when: putting incorrect credentials
  // then: error when putting credentials
  await notValidLogIn(page, 'alice@gmail.com', '123', 'Credentials are invalid.');

  // when: providing correct credentials
  await identityInput.fill(`user-${userDescriminator}`);
  await passwordLoginInput.fill('Random123');
  await loginButton.click();

  // then me page appears
  await expect(page.getByText(`Hello, user-${userDescriminator}`)).toBeVisible();
}

test('can authenticate with signup and login', async ({ browser }) => {
  // given: user1's browser
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto(website);

  // when: he can sign up
  const user1Id = getRandomInt();
  await canSignUp(page, user1Id);

  // then: he can logout
  await canLogOut(page);

  // and: log back in
  await canLogin(page, user1Id);
});
