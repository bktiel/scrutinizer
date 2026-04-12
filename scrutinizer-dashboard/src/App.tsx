import { BrowserRouter, Routes, Route, Link } from 'react-router-dom'
import { AppBar, Toolbar, Typography, Container, Box, Button, Stack } from '@mui/material'
import SecurityIcon from '@mui/icons-material/Security'
import DashboardIcon from '@mui/icons-material/Dashboard'
import FolderIcon from '@mui/icons-material/Folder'
import PolicyIcon from '@mui/icons-material/Policy'
import BlockIcon from '@mui/icons-material/Block'
import AddIcon from '@mui/icons-material/Add'
import OverviewDashboardPage from './pages/OverviewDashboardPage'
import ProjectsPage from './pages/ProjectsPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import DashboardPage from './pages/DashboardPage'
import RunDetailPage from './pages/RunDetailPage'
import FindingsPage from './pages/FindingsPage'
import ApplicationTrendPage from './pages/ApplicationTrendPage'
import PoliciesPage from './pages/PoliciesPage'
import PolicyConfiguratorPage from './pages/PolicyConfiguratorPage'
import NewRunPage from './pages/NewRunPage'
import ExceptionsPage from './pages/ExceptionsPage'

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
            <Button
              color="inherit"
              component={Link}
              to="/"
              startIcon={<DashboardIcon />}
              sx={{ textTransform: 'none' }}
            >
              Dashboard
            </Button>
            <Button
              color="inherit"
              component={Link}
              to="/projects"
              startIcon={<FolderIcon />}
              sx={{ textTransform: 'none' }}
            >
              Projects
            </Button>
            <Button
              color="inherit"
              component={Link}
              to="/policies"
              startIcon={<PolicyIcon />}
              sx={{ textTransform: 'none' }}
            >
              Policies
            </Button>
            <Button
              color="inherit"
              component={Link}
              to="/exceptions"
              startIcon={<BlockIcon />}
              sx={{ textTransform: 'none' }}
            >
              Exceptions
            </Button>
            <Button
              color="inherit"
              component={Link}
              to="/new-run"
              startIcon={<AddIcon />}
              sx={{ textTransform: 'none' }}
            >
              New Run
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="xl" sx={{ mt: 3, mb: 3 }}>
        <Box>
          <Routes>
            <Route path="/" element={<OverviewDashboardPage />} />
            <Route path="/projects" element={<ProjectsPage />} />
            <Route path="/projects/:id" element={<ProjectDetailPage />} />
            <Route path="/policies" element={<PoliciesPage />} />
            <Route path="/policies/new" element={<PolicyConfiguratorPage />} />
            <Route path="/policies/:id/edit" element={<PolicyConfiguratorPage />} />
            <Route path="/exceptions" element={<ExceptionsPage />} />
            <Route path="/new-run" element={<NewRunPage />} />
            <Route path="/runs/:id" element={<RunDetailPage />} />
            <Route path="/runs/:id/findings" element={<FindingsPage />} />
            <Route path="/trends/:applicationName" element={<ApplicationTrendPage />} />
            <Route path="/dashboard/runs" element={<DashboardPage />} />
          </Routes>
        </Box>
      </Container>
    </BrowserRouter>
  )
}
