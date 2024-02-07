import * as React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { fetchHome } from './http/Http';

const root = createRoot(document.getElementById('main-div'));

fetchHome();

root.render(<App />);
