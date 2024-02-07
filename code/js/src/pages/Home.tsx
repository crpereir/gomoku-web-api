import * as React from 'react';
import { Link } from 'react-router-dom';

/**
 * Component that describes the home page.
 */
export function Home() {
  console.log('Home Page');
  return (
    <div>
      <h1>Gomoku</h1>
      <hr></hr>
      <ol>
        <></>
        <p>
          <Link to="/me">Play Game</Link>
        </p>
        <p>
          <Link to="/ranking">Ranking</Link>
        </p>
      </ol>
    </div>
  );
}
