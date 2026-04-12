import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Typography,
  Card,
  CardContent,
  CardActions,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Grid,
  LinearProgress,
  Chip,
  Stack,
  CircularProgress,
  Paper,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import SignalBadge from '../components/SignalBadge'
import { listProjects, createProject, listPolicies, Project, Policy } from '../api/scrutinizerApi'

export default function ProjectsPage() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [policies, setPolicies] = useState<Policy[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    repositoryUrl: '',
    gitlabProjectId: '',
    defaultBranch: 'main',
    policyId: '',
  })
  const [filterText, setFilterText] = useState('')
  const [sortBy, setSortBy] = useState<'name' | 'score' | 'lastRun'>('name')

  useEffect(() => {
    Promise.all([listProjects(), listPolicies()])
      .then(([projectsData, policiesData]) => {
        setProjects(projectsData)
        setPolicies(policiesData)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const handleCreateProject = async () => {
    if (!formData.name.trim()) return
    try {
      const newProject = await createProject({
        name: formData.name,
        description: formData.description || undefined,
        repositoryUrl: formData.repositoryUrl || undefined,
        gitlabProjectId: formData.gitlabProjectId || undefined,
        defaultBranch: formData.defaultBranch,
        policyId: formData.policyId || undefined,
      })
      setProjects([...projects, newProject])
      setDialogOpen(false)
      setFormData({
        name: '',
        description: '',
        repositoryUrl: '',
        gitlabProjectId: '',
        defaultBranch: 'main',
        policyId: '',
      })
    } catch (error) {
      console.error('Failed to create project:', error)
    }
  }

  const filteredProjects = projects.filter((p) =>
    p.name.toLowerCase().includes(filterText.toLowerCase()) ||
    (p.description?.toLowerCase().includes(filterText.toLowerCase()) ?? false)
  )

  const sortedProjects = [...filteredProjects].sort((a, b) => {
    if (sortBy === 'name') {
      return a.name.localeCompare(b.name)
    } else if (sortBy === 'score') {
      const scoreA = a.stats?.latestScore ?? 0
      const scoreB = b.stats?.latestScore ?? 0
      return scoreB - scoreA
    } else if (sortBy === 'lastRun') {
      const dateA = a.stats?.lastRunAt ? new Date(a.stats.lastRunAt).getTime() : 0
      const dateB = b.stats?.lastRunAt ? new Date(b.stats.lastRunAt).getTime() : 0
      return dateB - dateA
    }
    return 0
  })

  const getCoverageColor = (coverage: number): 'success' | 'warning' | 'error' => {
    if (coverage > 80) return 'success'
    if (coverage > 50) return 'warning'
    return 'error'
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Projects</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          Register Project
        </Button>
      </Box>

      <Paper sx={{ p: 2, mb: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
        <TextField
          placeholder="Search projects..."
          size="small"
          value={filterText}
          onChange={(e) => setFilterText(e.target.value)}
          sx={{ flex: 1, maxWidth: 300 }}
        />
        <FormControl sx={{ minWidth: 150 }} size="small">
          <InputLabel>Sort By</InputLabel>
          <Select
            label="Sort By"
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'name' | 'score' | 'lastRun')}
          >
            <MenuItem value="name">Name</MenuItem>
            <MenuItem value="score">Latest Score</MenuItem>
            <MenuItem value="lastRun">Last Run</MenuItem>
          </Select>
        </FormControl>
      </Paper>

      <Grid container spacing={2}>
        {sortedProjects.map((project) => (
          <Grid item xs={12} sm={6} md={4} key={project.id}>
            <Card
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                cursor: 'pointer',
                '&:hover': {
                  borderColor: '#00B8D4',
                  boxShadow: '0 0 20px rgba(0, 184, 212, 0.1)',
                },
              }}
              onClick={() => navigate(`/projects/${project.id}`)}
            >
              <CardContent sx={{ flex: 1 }}>
                <Typography variant="h6" gutterBottom sx={{ fontWeight: 700 }}>
                  {project.name}
                </Typography>

                {project.description && (
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1, minHeight: 40 }}>
                    {project.description}
                  </Typography>
                )}

                {project.repositoryUrl && (
                  <Typography
                    variant="caption"
                    sx={{
                      display: 'block',
                      mb: 2,
                      color: '#00B8D4',
                      textDecoration: 'none',
                      '&:hover': { textDecoration: 'underline' },
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                    component="a"
                    href={project.repositoryUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {project.repositoryUrl}
                  </Typography>
                )}

                <Box sx={{ mb: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Policy
                  </Typography>
                  <Typography variant="body2">
                    {project.policyName || 'No policy assigned'}
                  </Typography>
                </Box>

                {project.stats && (
                  <Box sx={{ mb: 2 }}>
                    <Box sx={{ display: 'flex', gap: 1, mb: 1, alignItems: 'center' }}>
                      <Typography variant="caption" color="text.secondary">
                        Latest Score
                      </Typography>
                      <SignalBadge value={project.stats.latestScore} type="score" />
                      <SignalBadge value={project.stats.latestDecision} type="decision" />
                    </Box>

                    <Box sx={{ display: 'flex', gap: 2, mb: 1, flexWrap: 'wrap' }}>
                      <Chip
                        label={`${project.stats.totalRuns} runs`}
                        size="small"
                        variant="outlined"
                      />
                      <Chip
                        label={`${project.stats.totalComponents} components`}
                        size="small"
                        variant="outlined"
                      />
                    </Box>

                    <Box sx={{ mb: 1.5 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          Pass/Warn/Fail
                        </Typography>
                        <Typography variant="caption">
                          {project.stats.passCount}/{project.stats.warnCount}/{project.stats.failCount}
                        </Typography>
                      </Box>
                      <Box sx={{ display: 'flex', height: 8, gap: 1, borderRadius: 1, overflow: 'hidden' }}>
                        {project.stats.totalComponents > 0 && (
                          <>
                            <Box
                              sx={{
                                flex: project.stats.passCount / project.stats.totalComponents,
                                backgroundColor: '#00E676',
                              }}
                            />
                            <Box
                              sx={{
                                flex: project.stats.warnCount / project.stats.totalComponents,
                                backgroundColor: '#FFAB00',
                              }}
                            />
                            <Box
                              sx={{
                                flex: project.stats.failCount / project.stats.totalComponents,
                                backgroundColor: '#FF5252',
                              }}
                            />
                          </>
                        )}
                      </Box>
                    </Box>

                    <Box sx={{ mb: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          Provenance Coverage
                        </Typography>
                        <Typography variant="caption">
                          {project.stats.provenanceCoverage.toFixed(0)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={project.stats.provenanceCoverage}
                        color={getCoverageColor(project.stats.provenanceCoverage)}
                        sx={{ height: 6, borderRadius: 3 }}
                      />
                    </Box>

                    <Box>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          Scorecard Coverage
                        </Typography>
                        <Typography variant="caption">
                          {project.stats.scorecardCoverage.toFixed(0)}%
                        </Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={project.stats.scorecardCoverage}
                        color={getCoverageColor(project.stats.scorecardCoverage)}
                        sx={{ height: 6, borderRadius: 3 }}
                      />
                    </Box>

                    {project.stats.lastRunAt && (
                      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                        Last run: {new Date(project.stats.lastRunAt).toLocaleString()}
                      </Typography>
                    )}
                  </Box>
                )}
              </CardContent>

              <CardActions>
                <Button size="small" onClick={() => navigate(`/projects/${project.id}`)}>
                  View Details
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {sortedProjects.length === 0 && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {filterText ? 'No projects match your search.' : 'No projects yet. Create one to get started.'}
          </Typography>
        </Paper>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Register New Project</DialogTitle>
        <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Project Name"
            fullWidth
            required
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
          />
          <TextField
            label="Description"
            fullWidth
            multiline
            rows={2}
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
          />
          <TextField
            label="Repository URL"
            fullWidth
            type="url"
            value={formData.repositoryUrl}
            onChange={(e) => setFormData({ ...formData, repositoryUrl: e.target.value })}
          />
          <TextField
            label="GitLab Project ID"
            fullWidth
            value={formData.gitlabProjectId}
            onChange={(e) => setFormData({ ...formData, gitlabProjectId: e.target.value })}
          />
          <TextField
            label="Default Branch"
            fullWidth
            value={formData.defaultBranch}
            onChange={(e) => setFormData({ ...formData, defaultBranch: e.target.value })}
          />
          <FormControl fullWidth>
            <InputLabel>Policy (Optional)</InputLabel>
            <Select
              label="Policy (Optional)"
              value={formData.policyId}
              onChange={(e) => setFormData({ ...formData, policyId: e.target.value })}
            >
              <MenuItem value="">None</MenuItem>
              {policies.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name} v{p.version}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateProject}
            disabled={!formData.name.trim()}
          >
            Create Project
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
