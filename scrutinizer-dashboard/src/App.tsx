import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import { AppBar, Toolbar, Typography, Container, Box, Button, Stack } from '@mui/material'
import SecurityIcon from '@mui/icons-material/Security'
import DashboardPage from './pages/DashboardPage'
import RunDetailPage from './pages/RunDetailPage'
import FindingsPage from './pages/FindingsPage'
import ApplicationTrendPage from './pages/ApplicationTrendPage'
import PoliciesPage from './pages/PoliciesPage'
import NewRunPage from './pages/NewRunPage'

export default function App() {
  return (
    <BrowserRouter>
      <AppBar position="static" elevation={1}>
        <Toolbar>
          <SecurityIcon sx={{ mr: 1 }} />
          <Typography variant="h6" component="div" sx={{ mr: 3 }}>
            Scrutinizer
          </Typography>
          <Stack direction="row" spacing={1} sx={{ flexGrow: 1 }}>
            <Button color="inherit" component={Link} to="/">Runs</Button>
            <Button color="inherit" component={Link} to="/policies">Policies</Button>
            <Button color="inherit" component={Link} to="/new-run">New Run</Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" sx={{ mt: 3, mb: 3 }}>
        <Box>
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/policies" element={<PoliciesPage />} />
            <Route path="/new-run" element={<NewRunPage />} />
            <Route path="/runs/:id" element={<RunDetailPage />} />
            <Route path="/runs/:id/findings" element={<FindingsPage />} />
            <Route path="/trends/:applicationName" element={<ApplicationTrendPage />} />
          </Routes>
        </Box>
      </Container>
    </BrowserRouter>
  )
}
