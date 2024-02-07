import { Game } from "../../domain/Game";
import { Monitor } from "../../domain/Monitor";
import { Ranking } from "../../domain/Ranking";
import { User } from "../../domain/User";
import { UserStatistics } from "../../domain/UserStatistics";
import { Variant, Variants } from "../../domain/Variants";

export type PropertyTag = 'ranking' | 'userStatistics' | 'user' | 'variants' | 'start' | 'monitor' | 'game' | 'variant';

export type Property =
  | { tag: 'ranking'; value: Ranking }
  | { tag: 'userStatistics'; value: UserStatistics }
  | { tag: 'user'; value: User }
  | { tag: 'variants'; value: Variants }
  | { tag: 'start'; value?: Monitor }
  | { tag: 'monitor'; value: Monitor }
  | { tag: 'game'; value: Game }
  | { tag: 'variant'; value: Variant };

export type SirenLink = { rel: Array<string>; href: string; authenticationType: Array<string> };

export type SirenField = { name: string; type: string; value: string | null };

export type SirenAction = {
  name: string;
  title?: string;
  href: string;
  method: string;
  authenticationType: Array<string>;
  type?: string;
  fields: Array<SirenField>;
};

export type SirenEntity = {
  properties: Property;
  links: Array<SirenLink>;
  rel: Array<string>;
};

export type Siren = {
  clazz: Array<string>;
  properties: Property;
  links: Array<SirenLink>;
  entities: Array<SirenEntity>;
  actions: Array<SirenAction>;
};

export function getLink(links: Array<SirenLink>, rel: string): SirenLink {
  return links.find(l => l.rel.includes(rel));
}

export function getAction(actions: Array<SirenAction>, name: string): SirenAction {
  return actions.find(a => a.name === name);
}
