import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Typography,
  Tabs,
  Tab,
  Paper,
  Grid,
  Card,
  CardContent,
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
  CircularProgress,
  LinearProgress,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material'
import SignalBadge from '../components/SignalBadge'
import DependencyTable from '../components/DependencyTable'
import PostureTrendChart from '../components/PostureTrendChart'
import PolicyYamlPreview from '../components/PolicyYamlPreview'
import {
  getProject,
  Project,
  ProjectStats,
  getProjectTrends,
  TrendDataPoint,
  getProjectComponents,
  ComponentResult,
  getProjectRuns,
  PostureRunSummary,
  Page,
  getProjectExceptions,
  createException,
  deleteException,
  listPolicies,
  getPolicy,
  assignProjectPolicy,
  PolicyException,
  Policy,
} from '../api/scrutinizerApi'

export default function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [project, setProject] = useState<Project | null>(null)
  const [trends, setTrends] = useState<TrendDataPoint[]>([])
  const [components, setComponents] = useState<ComponentResult[]>([])
  const [runs, setRuns] = useState<PostureRunSummary[]>([])
  const [exceptions, setExceptions] = useState<PolicyException[]>([])
  const [policies, setPolicies] = useState<Policy[]>([])
  const [currentPolicy, setCurrentPolicy] = useState<Policy | null>(null)
  const [loading, setLoading] = useState(true)
  const [tab, setTab] = useState(0)
  const [exceptionDialogOpen, setExceptionDialogOpen] = useState(false)
  const [policyDialogOpen, setPolicyDialogOpen] = useState(false)
  const [exceptionForm, setExceptionForm] = useState({
    ruleId: '',
    packageName: '',
    packageVersion: '',
    justification: '',
    scope: 'PROJECT',
    expiresAt: '',
  })
  const [selectedPolicyId, setSelectedPolicyId] = useState('')

  useEffect(() => {
    if (!id) return

    Promise.all([
      getProject(id),
      listPolicies(),
    ])
      .then(async ([projectData, policiesData]) => {
        setProject(projectData)
        setPolicies(policiesData)

        if (projectData.policyId) {
          try {
            const policy = await getPolicy(projectData.policyId)
            setCurrentPolicy(policy)
          } catch (e) {
            console.error('Failed to fetch policy:', e)
          }
        }

        try {
          const trendsData = await getProjectTrends(id)
          setTrends(trendsData)
        } catch (e) {
          console.error('Failed to fetch trends:', e)
        }

        try {
          const componentsData = await getProjectComponents(id)
          setComponents(componentsData)
        } catch (e) {
          console.error('Failed to fetch components:', e)
        }

        try {
          const runsData = await getProjectRuns(id, 0, 20)
          setRuns(runsData.content)
        } catch (e) {
          console.error('Failed to fetch runs:', e)
        }

        try {
          const exceptionsData = await getProjectExceptions(id)
          setExceptions(exceptionsData)
        } catch (e) {
          console.error('Failed to fetch exceptions:', e)
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [id])

  const handleCreateException = async () => {
    if (!id || !exceptionForm.justification.trim()) return
    try {
      const newException = await createException({
        projectId: id,
        ruleId: exceptionForm.ruleId || undefined,
        packageName: exceptionForm.packageName || undefined,
        packageVersion: exceptionForm.packageVersion || undefined,
        justification: exceptionForm.justification,
        scope: exceptionForm.scope,
        expiresAt: exceptionForm.expiresAt || undefined,
      })
      setExceptions([...exceptions, newException])
      setExceptionDialogOpen(false)
      setExceptionForm({
        ruleId: '',
        packageName: '',
        packageVersion: '',
        justification: '',
        scope: 'PROJECT',
        expiresAt: '',
      })
    } catch (error) {
      console.error('Failed to create exception:', error)
    }
  }

  const handleRevokeException = async (exceptionId: string) => {
    try {
      await deleteException(exceptionId)
      setExceptions(exceptions.filter((e) => e.id !== exceptionId))
    } catch (error) {
      console.error('Failed to revoke exception:', error)
    }
  }

  const handleChangePolicy = async () => {
    if (!id || !selectedPolicyId) return
    try {
      const updatedProject = await assignProjectPolicy(id, selectedPolicyId)
      setProject(updatedProject)
      if (selectedPolicyId) {
        const policy = await getPolicy(selectedPolicyId)
        setCurrentPolicy(policy)
      }
      setPolicyDialogOpen(false)
    } catch (error) {
      console.error('Failed to change policy:', error)
    }
  }

  if (loading || !project) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    )
  }

  const stats = project.stats

  return (
    <Box>
      <Button onClick={() => navigate('/projects')} sx={{ mb: 2 }}>
        &larr; Back to Projects
      </Button>

      <Typography variant="h4" gutterBottom>
        {project.name}
      </Typography>

      {project.description && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          {project.description}
        </Typography>
      )}

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 3 }}>
        <Tab label="Overview" />
        <Tab label="Policy" />
        <Tab label="Exceptions" />
        <Tab label="Dependencies" />
        <Tab label="Runs" />
        <Tab label="Audit Bundles" />
      </Tabs>

      {tab === 0 && (
        <Box>
          {stats && (
            <Box>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} sm={6} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Latest Score
                      </Typography>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                        <Typography variant="h4">{stats.latestScore.toFixed(1)}</Typography>
                        <SignalBadge value={stats.latestDecision} type="decision" />
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Total Runs
                      </Typography>
                      <Typography variant="h4">{stats.totalRuns}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Total Components
                      </Typography>
                      <Typography variant="h4">{stats.totalComponents}</Typography>
                    </CardContent>
                  </Card>
                </Grid>
                <Grid item xs={12} sm={6} md={3}>
                  <Card>
                    <CardContent>
                      <Typography color="text.secondary" gutterBottom>
                        Provenance Coverage
                      </Typography>
                      <Typography variant="h4">{stats.provenanceCoverage.toFixed(0)}%</Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>

              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} md={6}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        Results Breakdown
                      </Typography>
                      <Box sx={{ mt: 2 }}>
                        <Box sx={{ mb: 2 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="body2">Pass</Typography>
                            <Typography variant="body2">{stats.passCount}</Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={(stats.passCount / stats.totalComponents) * 100}
                            color="success"
                            sx={{ height: 8, borderRadius: 4 }}
                          />
                        </Box>
                        <Box sx={{ mb: 2 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="body2">Warn</Typography>
                            <Typography variant="body2">{stats.warnCount}</Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={(stats.warnCount / stats.totalComponents) * 100}
                            color="warning"
                            sx={{ height: 8, borderRadius: 4 }}
                          />
                        </Box>
                        <Box>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="body2">Fail</Typography>
                            <Typography variant="body2">{stats.failCount}</Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={(stats.failCount / stats.totalComponents) * 100}
                            color="error"
                            sx={{ height: 8, borderRadius: 4 }}
                          />
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>

                <Grid item xs={12} md={6}>
                  <Card>
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        Coverage Metrics
                      </Typography>
                      <Box sx={{ mt: 2 }}>
                        <Box sx={{ mb: 2 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="body2">Provenance</Typography>
                            <Typography variant="body2">{stats.provenanceCoverage.toFixed(0)}%</Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={stats.provenanceCoverage}
                            color={stats.provenanceCoverage > 80 ? 'success' : stats.provenanceCoverage > 50 ? 'warning' : 'error'}
                            sx={{ height: 8, borderRadius: 4 }}
                          />
                        </Box>
                        <Box>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="body2">Scorecard</Typography>
                            <Typography variant="body2">{stats.scorecardCoverage.toFixed(0)}%</Typography>
                          </Box>
                          <LinearProgress
                            variant="determinate"
                            value={stats.scorecardCoverage}
                            color={stats.scorecardCoverage > 80 ? 'success' : stats.scorecardCoverage > 50 ? 'warning' : 'error'}
                            sx={{ height: 8, borderRadius: 4 }}
                          />
                        </Box>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>

              {trends.length > 0 && (
                <Card sx={{ mb: 3 }}>
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      Score Trend
                    </Typography>
                    <PostureTrendChart data={trends} />
                  </CardContent>
                </Card>
              )}

              {stats.lastRunAt && (
                <Typography variant="caption" color="text.secondary">
                  Last evaluated: {new Date(stats.lastRunAt).toLocaleString()}
                </Typography>
              )}
            </Box>
          )}
        </Box>
      )}

      {tab === 1 && (
        <Box>
          <Card>
            <CardContent>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h6">Assigned Policy</Typography>
                <Button variant="outlined" size="small" onClick={() => setPolicyDialogOpen(true)}>
                  Change Policy
                </Button>
              </Box>

              {currentPolicy ? (
                <Box>
                  <Typography variant="body2" sx={{ mb: 1 }}>
                    <strong>{currentPolicy.name}</strong> v{currentPolicy.version}
                  </Typography>
                  {currentPolicy.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                      {currentPolicy.description}
                    </Typography>
                  )}
                  <PolicyYamlPreview yaml={currentPolicy.policyYaml} />
                </Box>
              ) : (
                <Typography color="text.secondary">No policy assigned</Typography>
              )}
            </CardContent>
          </Card>
        </Box>
      )}

      {tab === 2 && (
        <Box>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Policy Exceptions</Typography>
            <Button
              variant="contained"
              color="primary"
              size="small"
              onClick={() => setExceptionDialogOpen(true)}
            >
              Create Exception
            </Button>
          </Box>

          {exceptions.length > 0 ? (
            <Paper>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#0D1117' }}>
                    <TableCell>Rule ID</TableCell>
                    <TableCell>Package</TableCell>
                    <TableCell>Justification</TableCell>
                    <TableCell>Created By</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Expires</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {exceptions.map((exc) => (
                    <TableRow key={exc.id}>
                      <TableCell>{exc.ruleId || '-'}</TableCell>
                      <TableCell>
                        {exc.packageName ? `${exc.packageName}${exc.packageVersion ? `@${exc.packageVersion}` : ''}` : '-'}
                      </TableCell>
                      <TableCell>{exc.justification}</TableCell>
                      <TableCell>{exc.createdBy}</TableCell>
                      <TableCell>
                        <Chip label={exc.status} size="small" />
                      </TableCell>
                      <TableCell>{exc.expiresAt ? new Date(exc.expiresAt).toLocaleDateString() : 'Never'}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          color="error"
                          onClick={() => handleRevokeException(exc.id)}
                        >
                          Revoke
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          ) : (
            <Typography color="text.secondary">No exceptions yet.</Typography>
          )}

          <Dialog open={exceptionDialogOpen} onClose={() => setExceptionDialogOpen(false)} maxWidth="sm" fullWidth>
            <DialogTitle>Create Policy Exception</DialogTitle>
            <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
              <TextField
                label="Rule ID (Optional)"
                fullWidth
                value={exceptionForm.ruleId}
                onChange={(e) => setExceptionForm({ ...exceptionForm, ruleId: e.target.value })}
              />
              <TextField
                label="Package Name (Optional)"
                fullWidth
                value={exceptionForm.packageName}
                onChange={(e) => setExceptionForm({ ...exceptionForm, packageName: e.target.value })}
              />
              <TextField
                label="Package Version (Optional)"
                fullWidth
                value={exceptionForm.packageVersion}
                onChange={(e) => setExceptionForm({ ...exceptionForm, packageVersion: e.target.value })}
              />
              <TextField
                label="Justification"
                fullWidth
                required
                multiline
                rows={3}
                value={exceptionForm.justification}
                onChange={(e) => setExceptionForm({ ...exceptionForm, justification: e.target.value })}
              />
              <FormControl fullWidth>
                <InputLabel>Scope</InputLabel>
                <Select
                  label="Scope"
                  value={exceptionForm.scope}
                  onChange={(e) => setExceptionForm({ ...exceptionForm, scope: e.target.value })}
                >
                  <MenuItem value="PROJECT">Project</MenuItem>
                  <MenuItem value="GLOBAL">Global</MenuItem>
                </Select>
              </FormControl>
              <TextField
                label="Expires At (Optional)"
                fullWidth
                type="datetime-local"
                InputLabelProps={{ shrink: true }}
                value={exceptionForm.expiresAt}
                onChange={(e) => setExceptionForm({ ...exceptionForm, expiresAt: e.target.value })}
              />
            </DialogContent>
            <DialogActions>
              <Button onClick={() => setExceptionDialogOpen(false)}>Cancel</Button>
              <Button
                variant="contained"
                onClick={handleCreateException}
                disabled={!exceptionForm.justification.trim()}
              >
                Create Exception
              </Button>
            </DialogActions>
          </Dialog>
        </Box>
      )}

      {tab === 3 && (
        <Box>
          {components.length > 0 ? (
            <DependencyTable components={components} />
          ) : (
            <Typography color="text.secondary">No components found.</Typography>
          )}
        </Box>
      )}

      {tab === 4 && (
        <Box>
          {runs.length > 0 ? (
            <Paper>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#0D1117' }}>
                    <TableCell>Decision</TableCell>
                    <TableCell>Score</TableCell>
                    <TableCell>Policy</TableCell>
                    <TableCell>Evaluated</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {runs.map((run) => (
                    <TableRow key={run.id}>
                      <TableCell>
                        <SignalBadge value={run.overallDecision} type="decision" />
                      </TableCell>
                      <TableCell>
                        <SignalBadge value={run.postureScore} type="score" />
                      </TableCell>
                      <TableCell>
                        {run.policyName} v{run.policyVersion}
                      </TableCell>
                      <TableCell>{new Date(run.runTimestamp).toLocaleString()}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          onClick={() => navigate(`/runs/${run.id}`)}
                        >
                          View
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          ) : (
            <Typography color="text.secondary">No runs yet.</Typography>
          )}
        </Box>
      )}

      {tab === 5 && (
        <Box>
          {runs.length > 0 ? (
            <Paper>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: '#0D1117' }}>
                    <TableCell>Run ID</TableCell>
                    <TableCell>Decision</TableCell>
                    <TableCell>Score</TableCell>
                    <TableCell>Evaluated</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {runs.map((run) => (
                    <TableRow key={run.id}>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>
                        {run.id.substring(0, 8)}...
                      </TableCell>
                      <TableCell>
                        <SignalBadge value={run.overallDecision} type="decision" />
                      </TableCell>
                      <TableCell>
                        <SignalBadge value={run.postureScore} type="score" />
                      </TableCell>
                      <TableCell>{new Date(run.runTimestamp).toLocaleString()}</TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          onClick={() => navigate(`/runs/${run.id}`)}
                        >
                          View Evidence
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          ) : (
            <Typography color="text.secondary">No audit bundles available.</Typography>
          )}
        </Box>
      )}

      <Dialog open={policyDialogOpen} onClose={() => setPolicyDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Change Policy</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <FormControl fullWidth>
            <InputLabel>Select Policy</InputLabel>
            <Select
              label="Select Policy"
              value={selectedPolicyId}
              onChange={(e) => setSelectedPolicyId(e.target.value)}
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
          <Button onClick={() => setPolicyDialogOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleChangePolicy}>
            Assign Policy
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
