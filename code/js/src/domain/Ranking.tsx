import { UserStatistics } from "./UserStatistics";

export type Ranking = {
    totalUsers: number;
    ranking: Array<UserStatistics>;
  };