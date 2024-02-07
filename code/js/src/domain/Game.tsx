
export type GameCurrentState = 'NEXT_TURN_W' | 'NEXT_TURN_B' | 'WINNER_W' | 'WINNER_B' | 'DRAW';

export type Player = 'B' | 'W';

export type Intersection = {
  row: number;
  column: number;
};

export type Piece = {
  player: Player;
  intersection: Intersection;
};

export type Game = {
  gameState: GameCurrentState;
  playerB: number;
  playerW: number;
  playingRoundTime: string;
  pieces: Array<Piece>;
};
