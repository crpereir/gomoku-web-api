import { SirenLink, getLink } from '../infra/Siren';

export type Page = {
  type: 'singlePage' | 'firstPage' | 'middlePage' | 'lastPage';
  firstHref?: string;
  previousHref?: string;
  nextHref?: string;
  lastHref?: string;
};

export function definePage(links: Array<SirenLink>): Page {
  const firstLink = getLink(links, 'first');
  const lastLink = getLink(links, 'last');
  const nextLink = getLink(links, 'next');
  const previousLink = getLink(links, 'previous');

  switch (true) {
    case firstLink !== undefined && lastLink !== undefined && nextLink !== undefined && previousLink !== undefined: {
      const middlePage: Page = {
        type: 'middlePage',
        firstHref: firstLink.href,
        previousHref: previousLink.href,
        nextHref: nextLink.href,
        lastHref: lastLink.href,
      };
      return middlePage;
    }
    case nextLink !== undefined && lastLink !== undefined: {
      const firstPage: Page = { type: 'firstPage', nextHref: nextLink.href, lastHref: lastLink.href };
      return firstPage;
    }

    case previousLink !== undefined && firstLink !== undefined: {
      const lastPage: Page = { type: 'lastPage', previousHref: previousLink.href, firstHref: firstLink.href };
      return lastPage;
    }

    default: {
      const singlePage: Page = { type: 'singlePage' };
      return singlePage;
    }
  }
}
