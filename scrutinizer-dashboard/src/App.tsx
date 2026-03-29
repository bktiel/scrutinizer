import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AppBar, Toolbar, Typography, Container, Box } from '@mui/material'
import SecurityIcon from '@mui/icons-material/Security'
import DashboardPage from './pages/DashboardPage'
import RunDetailPage from './pages/RunDetailPage'
import FindingsPage from './pages/FindingsPage'
import ApplicationTrendPage from './pages/ApplicationTrendPage'

export default function App() {
  return (
    <BrowserRouter>
      <AppBar position="static" elevation={1}>
        <Toolbar>
          <SecurityIcon sx={{ mr: 1 }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
            Scrutinizer
          </Typography>
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" sx={{ mt: 3, mb: 3 }}>
        <Box>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/runs/:id" element={<RunDetailPage />} />
            <Route path="/runs/:id/findings" element={<FindingsPage />} />
            <Route path="/trends/:applicationName" element={<ApplicationTrendPage />} />
          </Routes>
        </Box>
      </Container>
    </BrowserRouter>
  )
}
