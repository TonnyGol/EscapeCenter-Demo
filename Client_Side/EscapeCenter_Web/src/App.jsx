import { MotionConfig } from 'framer-motion';
import './App.css';
import './theme.css';
import AppRoutes from './routes';

function App() {
  return (
    <MotionConfig reducedMotion="user">
      <AppRoutes />
    </MotionConfig>
  );
}

export default App;
