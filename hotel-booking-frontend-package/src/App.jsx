import { BrowserRouter, Routes, Route } from 'react-router-dom';
import HomePageSearch from './pages/HomePageSearch.jsx';
import SearchResultsPage from './pages/SearchResultsPage.jsx';
import Header from './components/Header';

function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<HomePageSearch />} />
        <Route path="/search" element={<SearchResultsPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;