import * as React from 'react';
import './css/style.css';
import { Piece } from '../domain/Game';

type BoardProps = {
  boardSize: number;
  pieces: Array<Piece>;
  onIntersectionClick?: (row: number, column: number) => void;
};

export function Board({ boardSize, pieces, onIntersectionClick }: BoardProps) {
  const board = () => {
    const cells = [];

    for (let r = 0; r < boardSize; r++) {
      for (let c = 0; c < boardSize; c++) {
        const piece = pieces.find(p => p.intersection.row === r && p.intersection.column === c);

        const intersectionClickHandler = () => {
          if (onIntersectionClick) {
            onIntersectionClick(r, c);
          }
        };

        cells.push(
          <div
            id={`${r}:${c}`}
            key={`${r}-${c}`}
            className={`intersection ${piece ? `player-${piece.player}` : ''}`}
            onClick={intersectionClickHandler}
          >
            {piece === undefined ? '   ' : piece.player === 'B' ? '  ' : piece.player === 'W' ? '  ' : '  '}
            {piece && <div className={`circle player-${piece.player}`}></div>}
          </div>
        );
      }
    }

    return cells;
  };

  return <div className={`board board-size-${boardSize}`}>{board()}</div>;
}

