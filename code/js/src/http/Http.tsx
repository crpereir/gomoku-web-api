import { Siren, PropertyTag, SirenAction, getAction, getLink, SirenLink } from '../utils/infra/Siren';
import { Problem, NoConnectionError, NotAuthorizedError } from '../utils/error/Problem';

const MEDIA_TYPE_SIREN = 'application/vnd.siren+json';
const MEDIA_TYPE_APPLICATION_JSON = 'application/json';
const MEDIA_TYPE_PROBLEM_JSON = 'application/problem+json';

const api = window.location.origin;

type HomeLoad = { tag: 'promise'; value: Promise<Result> } | { tag: 'siren'; value: Siren };

let homeLoad: HomeLoad;

function getHome(): Promise<Siren> | Siren {
  if (homeLoad.tag === 'promise') {
    return homeLoad.value.then(res => {
      if (res.tag === 'success') {
        const siren: HomeLoad = {
          tag: 'siren',
          value: res.value,
        };
        homeLoad = siren;
        return siren.value;
      } else {
        getHome();
      }
    });
  } else {
    return homeLoad.value;
  }
}

export function fetchHome() {
  const uri = api + '/api/';
  const options = buildOptions('GET');
  const promise: HomeLoad = {
    tag: 'promise',
    value: APIfetch(uri, options),
  };
  homeLoad = promise;
}

export type Result = { tag: 'success'; value?: Siren } | { tag: 'failure'; value: Problem };

function buildOptions(method: string, body?: object) {
  const headers = {
    'Content-Type': MEDIA_TYPE_APPLICATION_JSON,
    Accept: `${MEDIA_TYPE_APPLICATION_JSON}, ${MEDIA_TYPE_SIREN}, ${MEDIA_TYPE_PROBLEM_JSON}`,
  };
  const options: RequestInit = body
    ? {
        method: method,
        headers: headers,
        body: JSON.stringify(body),
      }
    : {
        method: method,
        headers: headers,
      };
  return options;
}

async function APIfetch(uri: string, options: RequestInit, sirenPropertiesTag?: PropertyTag): Promise<Result> {
  return fetch(uri, options)
    .then(async res => {
      if (res.ok && res.headers.get('Content-Type') === MEDIA_TYPE_SIREN) {
        return res.json().then(obj => {
          const siren: Siren = {
            clazz: obj.class,
            properties: { tag: sirenPropertiesTag, value: obj.properties },
            links: obj.links,
            actions: obj.actions,
            entities: obj.entities,
          };
          const result: Result = { tag: 'success', value: siren };
          return result;
        });
      } else if (res.ok) {
        const result: Result = { tag: 'success' };
        return result;
      } else if (res.body) {
        return res.json().then(error => {
          const problem: Problem = {
            type: error.type,
            title: error.title,
            detail: error.detail,
            instance: error.instance,
          };
          const result: Result = { tag: 'failure', value: problem };
          return result;
        });
      } else {
        const result: Result = { tag: 'failure', value: NotAuthorizedError };
        return result;
      }
    })
    .catch(err => {
      console.log(`error: ${err}`);
      const result: Result = { tag: 'failure', value: NoConnectionError };
      return result;
    });
}

export async function login(identity: string, password: string): Promise<Result> {
  const homeSiren = await getHome();
  const actionLogin: SirenAction = getAction(homeSiren.actions, 'login');

  const uri = api + actionLogin.href;
  const body = {
    [actionLogin.fields[0].name]: identity,
    [actionLogin.fields[1].name]: password,
  };
  const options = buildOptions(actionLogin.method, body);
  return APIfetch(uri, options);
}

export async function logout(action: SirenAction): Promise<Result> {
  const uri = api + action.href;
  const options = buildOptions(action.method);
  return APIfetch(uri, options);
}

export async function signUp(username: string, email: string, password: string): Promise<Result> {
  const homeSiren = await getHome();
  const actionSignUp: SirenAction = getAction(homeSiren.actions, 'signup');

  const uri = api + actionSignUp.href;
  const body = {
    [actionSignUp.fields[0].name]: username,
    [actionSignUp.fields[1].name]: email,
    [actionSignUp.fields[2].name]: password,
  };
  const options = buildOptions(actionSignUp.method, body);
  return APIfetch(uri, options).then(res => {
    console.log(res);
    if (res.tag === 'success'){
      return login(username, password);
    } else {
      return res
    }
  });
}

export async function fetchRanking(href?: string): Promise<Result> {
  const options = buildOptions('GET');
  if (!href) {
    const homeSiren = await getHome();
    const linkRanking: SirenLink = getLink(
      homeSiren.links,
      'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/ranking'
    );
    return APIfetch(api + linkRanking.href, options, 'ranking');
  } else {
    return APIfetch(api + href, options, 'ranking');
  }
}

export async function fetchUserStatistics(id: string): Promise<Result> {
  const homeSiren = await getHome();
  const linkUserStatistics: SirenLink = getLink(
    homeSiren.links,
    'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/statistics'
  );

  const uri = api + linkUserStatistics.href.replace('{id}', id);
  const options = buildOptions('GET');
  return APIfetch(uri, options, 'userStatistics');
}

export async function me(): Promise<Result> {
  const homeSiren = await getHome();
  const linkMe: SirenLink = getLink(
    homeSiren.links,
    'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/userHome'
  );

  const uri = api + linkMe.href;

  const options = buildOptions('GET');
  return APIfetch(uri, options, 'user');
}

export async function fetchVariants(link: SirenLink): Promise<Result> {
  const uri = api + link.href;
  const options = buildOptions('GET');
  return APIfetch(uri, options, 'variants');
}

export async function fetchVariant(link: SirenLink): Promise<Result> {
  const uri = api + link.href;
  const options = buildOptions('GET');
  return APIfetch(uri, options, 'variant');
}

export async function startGame(action: SirenAction, variantId: number): Promise<Result> {
  const uri = api + action.href;
  const body = { [action.fields[0].name]: variantId };
  const options = buildOptions(action.method, body);
  return APIfetch(uri, options, 'start');
}

export async function fetchMonitor(id: string): Promise<Result> {
  const homeSiren = await getHome();
  const linkMonitor: SirenLink = getLink(
    homeSiren.links,
    'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/monitor'
  );

  const uri = api + linkMonitor.href.replace('{id}', id);
  const options = buildOptions('GET');
  return APIfetch(uri, options, 'monitor');
}

export async function deleteMonitor(action: SirenAction, id: string): Promise<Result> {
  const uri = api + action.href.replace('{id}', id);
  const options = buildOptions(action.method);
  return APIfetch(uri, options);
}

export async function fetchGame(id: string): Promise<Result> {
  const homeSiren = await getHome();
  const linkGame: SirenLink = getLink(
    homeSiren.links,
    'https://github.com/isel-leic-daw/2023-daw-leic51d-07/tree/main/docs/api/rel/game'
  );

  const uri = api + linkGame.href.replace('{id}', id);
  const options = buildOptions('GET');
  return APIfetch(uri, options, 'game');
}

export async function play(action: SirenAction, id: string, row: number, column: number): Promise<Result> {
  const uri =
    api + action.href.replace('{id}', id).replace('{row}', row.toString()).replace('{column}', column.toString());
  const options = buildOptions(action.method);
  return APIfetch(uri, options, 'game');
}

export async function giveUp(action: SirenAction, id: string): Promise<Result> {
  const uri = api + action.href.replace('{id}', id);
  const options = buildOptions(action.method);
  return APIfetch(uri, options);
}
