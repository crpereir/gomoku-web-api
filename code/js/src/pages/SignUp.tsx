import * as React from 'react';
import { Navigate, useLocation, Link } from 'react-router-dom';
import { signUp } from '../http/Http';
import { error } from '../utils/error/Error';
import { Problem, UnexpectedError } from '../utils/error/Problem';
import { CustomForm, CustomFormProps, CustomLabelProps } from '../components/CustomForm';

/**
 * Sign up page states.
 */
export type SignUpState =
  | { tag: 'inputting'; error?: Problem; inputs: { username: string; email: string; password: string } }
  | { tag: 'processing'; username: string; email: string }
  | { tag: 'redirect' };

/**
 * Actions on sign up page states.
 */
export type SignUpAction =
  | { type: 'input'; inputName: string; inputValue: string }
  | { type: 'submit' }
  | { type: 'failure'; error: Problem }
  | { type: 'success' };

/**
 * Given a current state of the sign up page and an action produces a new state of the sign up page.
 * @param state
 * @param action
 * @returns the new state of the sign up page.
 * @throws if the action cannot be produced on the current page.
 */
function reduce(state: SignUpState, action: SignUpAction): SignUpState {
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
          return { tag: 'processing', username: state.inputs.username, email: state.inputs.email };
        default:
          error(state, action);
      }
      break;
    case 'processing':
      switch (action.type) {
        case 'success':
          return { tag: 'redirect' };
        case 'failure':
          return { tag: 'inputting', error: action.error, inputs: { username: '', email: '', password: '' } };
        default:
          error(state, action);
      }
      break;
    case 'redirect':
      error(state, action);
  }
}

/**
 * Component that describes the sign up page.
 */
export function SignUp() {
  console.log('SignUp Page');
  const [state, dispatch] = React.useReducer(reduce, {
    tag: 'inputting',
    inputs: { username: '', email: '', password: '' },
  });
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

    const username = state.inputs.username;
    const email = state.inputs.email;
    const password = state.inputs.password;
    signUp(username, email, password).then(res => {
      if (res.tag === 'success') {
        dispatch({ type: 'success' });
      } else {
        dispatch({ type: 'failure', error: res.value ? res.value : UnexpectedError });
      }
    });
  }

  const usernameLabelProps: CustomLabelProps = {
    key: 'username',
    for: 'username',
    name: 'Username ',
    inputId: 'username',
    inputName: 'username',
    inputType: 'text',
    inputValue: state.tag === 'processing' ? state.username : state.inputs.username,
  };

  const emailLabelProps: CustomLabelProps = {
    key: 'email',
    for: 'email',
    name: 'Email ',
    inputId: 'email',
    inputName: 'email',
    inputType: 'text',
    inputValue: state.tag === 'processing' ? state.email : state.inputs.email,
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
    labels: [usernameLabelProps, emailLabelProps, passwordLabelProps],
    buttonName: 'Sign up',
    handleChange: handleChange,
  };

  const loginLink =
    state.tag === 'inputting' ? (
      <p>
        Already have an account? <Link to="/login"> Login. </Link>
      </p>
    ) : undefined;

  const error = state.tag === 'inputting' && state.error && state.error.title;

  return (
    <div>
      <h1>SignUp</h1>
      <hr></hr>
      <CustomForm {...customFormProps} />
      <p>{error}</p>
      {loginLink}
    </div>
  );
}
