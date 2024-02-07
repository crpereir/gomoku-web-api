import * as React from 'react';
import { Navigate, useLocation, Link } from 'react-router-dom';
import { login } from '../http/Http';
import { error } from '../utils/error/Error';
import { Problem, UnexpectedError } from '../utils/error/Problem';
import { CustomForm, CustomFormProps, CustomLabelProps } from '../components/CustomForm';

/**
 * Login page states.
 */
export type LogInState =
  | { tag: 'inputting'; error?: Problem; inputs: { identity: string; password: string } }
  | { tag: 'processing'; identity: string }
  | { tag: 'redirect' };

/**
 * Actions on login page states.
 */
export type LogInAction =
  | { type: 'input'; inputName: string; inputValue: string }
  | { type: 'submit' }
  | { type: 'failure'; error: Problem }
  | { type: 'success' };

/**
 * Given a current state of the login page and an action produces a new state of the login page.
 * @param state
 * @param action
 * @returns the new state of the login page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: LogInState, action: LogInAction): LogInState {
  switch (state.tag) {
    case 'inputting':
      switch (action.type) {
        case 'input':
          return {
            tag: 'inputting',
            error: undefined,
            inputs: { ...state.inputs, [action.inputName]: action.inputValue },
          };
        case 'submit':
          return { tag: 'processing', identity: state.inputs.identity };
        default:
          error(state, action);
      }
      break;
    case 'processing':
      switch (action.type) {
        case 'success':
          return { tag: 'redirect' };
        case 'failure':
          return { tag: 'inputting', error: action.error, inputs: { identity: state.identity, password: '' } };
        default:
          error(state, action);
      }
      break;
    case 'redirect':
      error(state, action);
  }
}

/**
 * Component that describes the login page.
 */
export function Login() {
  console.log('Login Page');
  const [state, dispatch] = React.useReducer(reduce, { tag: 'inputting', inputs: { identity: '', password: '' } });
  const location = useLocation();

  if (state.tag === 'redirect') {
    return <Navigate to={location.state?.source?.pathname || '/me'} replace={true} />;
  }

  function handleChange(ev: React.FormEvent<HTMLInputElement>) {
    dispatch({ type: 'input', inputName: ev.currentTarget.name, inputValue: ev.currentTarget.value });
  }

  function handleSubmit(ev: React.FormEvent<HTMLFormElement>) {
    ev.preventDefault();
    if (state.tag !== 'inputting') {
      return;
    }
    dispatch({ type: 'submit' });
    const identity = state.inputs.identity;
    const password = state.inputs.password;
    login(identity, password).then(res => {
      if (res.tag === 'success') {
        dispatch({ type: 'success' });
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    });
  }

  const identityLabelProps: CustomLabelProps = {
    key: 'identity',
    for: 'identity',
    name: 'Username or Email ',
    inputId: 'identity',
    inputName: 'identity',
    inputType: 'text',
    inputValue: state.tag === 'processing' ? state.identity : state.inputs.identity,
  };

  const passwordLabelProps: CustomLabelProps = {
    key: 'password',
    for: 'password',
    name: 'Password ',
    inputId: 'password',
    inputName: 'password',
    inputType: 'password',
    inputValue: state.tag === 'processing' ? '' : state.inputs.password,
  };

  const customFormProps: CustomFormProps = {
    handleSubmit: handleSubmit,
    disabled: state.tag !== 'inputting',
    labels: [identityLabelProps, passwordLabelProps],
    buttonName: 'Login',
    handleChange: handleChange,
  };

  const signUpLink =
    state.tag === 'inputting' ? (
      <p>
        Do you not have an account yet? <Link to="/signUp">Sign up.</Link>
      </p>
    ) : undefined;

  const error = state.tag === 'inputting' && state.error && state.error.title;

  return (
    <div>
      <h1>Login</h1>
      <hr></hr>
      <CustomForm {...customFormProps} />
      <p>{error}</p>
      {signUpLink}
    </div>
  );
}
