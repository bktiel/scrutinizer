import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  CircularProgress,
  Chip,
  LinearProgress,
} from '@mui/material'
import SignalBadge from '../components/SignalBadge'
import { listProjects, listRuns, Project, PostureRunSummary, Page } from '../api/scrutinizerApi'

export default function OverviewDashboardPage() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [runs, setRuns] = useState<PostureRunSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([listProjects(), listRuns(0, 50)])
      .then(([projectsData, runsData]) => {
        setProjects(projectsData)
        setRuns(runsData.content)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
        <CircularProgress />
      </Box>
    )
  }

  // Calculate summary metrics
  const totalProjects = projects.length
  const avgScore = projects.length > 0
    ? projects.reduce((sum, p) => sum + (p.stats?.latestScore ?? 0), 0) / projects.length
    : 0

  const totalComponents = projects.reduce((sum, p) => sum + (p.stats?.totalComponents ?? 0), 0)
  const totalPass = projects.reduce((sum, p) => sum + (p.stats?.passCount ?? 0), 0)
  const passRate = totalComponents > 0 ? (totalPass / totalComponents) * 100 : 0

  const activeExceptions = 0 // TODO: fetch from backend if needed

  // Group projects by health
  const projectsByHealth = {
    PASS: projects.filter((p) => p.stats?.latestDecision === 'PASS'),
    WARN: projects.filter((p) => p.stats?.latestDecision === 'WARN'),
    FAIL: projects.filter((p) => p.stats?.latestDecision === 'FAIL'),
  }

  // Get top findings (aggregate findings from latest components)
  const findingsMap = new Map<string, number>()
  projects.forEach((p) => {
    p.stats?.failCount // This is aggregated fail count
  })

  const recentRuns = runs.slice(0, 5)

  return (
    <Box>
      <Typography variant="h4" gutterBottom sx={{ mb: 3 }}>
        Dashboard
      </Typography>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom sx={{ fontSize: '0.875rem' }}>
                Total Projects
              </Typography>
              <Typography variant="h4" sx={{ mt: 1 }}>
                {totalProjects}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom sx={{ fontSize: '0.875rem' }}>
                Average Score
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 1 }}>
                <Typography variant="h4">{avgScore.toFixed(1)}</Typography>
                <SignalBadge
                  value={avgScore > 7 ? 'PASS' : avgScore > 4 ? 'WARN' : 'FAIL'}
                  type="decision"
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom sx={{ fontSize: '0.875rem' }}>
                Pass Rate
              </Typography>
              <Box sx={{ mt: 1 }}>
                <Typography variant="h4" sx={{ mb: 1 }}>
                  {passRate.toFixed(1)}%
                </Typography>
                <LinearProgress
                  variant="determinate"
                  value={passRate}
                  color="success"
                  sx={{ height: 6, borderRadius: 3 }}
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="text.secondary" gutterBottom sx={{ fontSize: '0.875rem' }}>
                Active Exceptions
              </Typography>
              <Typography variant="h4" sx={{ mt: 1 }}>
                {activeExceptions}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Recent Activity
              </Typography>
              {recentRuns.length > 0 ? (
                <Paper>
                  <Table>
                    <TableHead>
                      <TableRow sx={{ backgroundColor: '#0D1117' }}>
                        <TableCell>Project</TableCell>
                        <TableCell>Decision</TableCell>
                        <TableCell>Score</TableCell>
                        <TableCell>Policy</TableCell>
                        <TableCell>Evaluated</TableCell>
                        <TableCell align="right">Action</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {recentRuns.map((run) => {
                        const project = projects.find((p) => p.name === run.applicationName)
                        return (
                          <TableRow key={run.id}>
                            <TableCell>
                              <Typography
                                sx={{
                                  cursor: 'pointer',
                                  color: '#00B8D4',
                                  '&:hover': { textDecoration: 'underline' },
                                }}
                                onClick={() => project && navigate(`/projects/${project.id}`)}
                              >
                                {run.applicationName}
                              </Typography>
                            </TableCell>
                            <TableCell>
                              <SignalBadge value={run.overallDecision} type="decision" />
                            </TableCell>
                            <TableCell>
                              <SignalBadge value={run.postureScore} type="score" />
                            </TableCell>
                            <TableCell>
                              {run.policyName} v{run.policyVersion}
                            </TableCell>
                            <TableCell sx={{ fontSize: '0.875rem' }}>
                              {new Date(run.runTimestamp).toLocaleString()}
                            </TableCell>
                            <TableCell align="right">
                              <Typography
                                sx={{
                                  cursor: 'pointer',
                                  color: '#00B8D4',
                                  fontSize: '0.875rem',
                                  '&:hover': { textDecoration: 'underline' },
                                }}
                                onClick={() => navigate(`/runs/${run.id}`)}
                              >
                                View
                              </Typography>
                            </TableCell>
                          </TableRow>
                        )
                      })}
                    </TableBody>
                  </Table>
                </Paper>
              ) : (
                <Typography color="text.secondary">No recent runs.</Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Projects by Health
              </Typography>

              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip label="PASS" color="success" size="small" />
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {projectsByHealth.PASS.length}
                  </Typography>
                </Box>
                {projectsByHealth.PASS.length > 0 && (
                  <Box sx={{ pl: 2 }}>
                    {projectsByHealth.PASS.map((p) => (
                      <Typography
                        key={p.id}
                        variant="caption"
                        sx={{
                          display: 'block',
                          color: '#00B8D4',
                          cursor: 'pointer',
                          '&:hover': { textDecoration: 'underline' },
                        }}
                        onClick={() => navigate(`/projects/${p.id}`)}
                      >
                        {p.name}
                      </Typography>
                    ))}
                  </Box>
                )}
              </Box>

              <Box sx={{ mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip label="WARN" color="warning" size="small" />
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {projectsByHealth.WARN.length}
                  </Typography>
                </Box>
                {projectsByHealth.WARN.length > 0 && (
                  <Box sx={{ pl: 2 }}>
                    {projectsByHealth.WARN.map((p) => (
                      <Typography
                        key={p.id}
                        variant="caption"
                        sx={{
                          display: 'block',
                          color: '#00B8D4',
                          cursor: 'pointer',
                          '&:hover': { textDecoration: 'underline' },
                        }}
                        onClick={() => navigate(`/projects/${p.id}`)}
                      >
                        {p.name}
                      </Typography>
                    ))}
                  </Box>
                )}
              </Box>

              <Box>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip label="FAIL" color="error" size="small" />
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {projectsByHealth.FAIL.length}
                  </Typography>
                </Box>
                {projectsByHealth.FAIL.length > 0 && (
                  <Box sx={{ pl: 2 }}>
                    {projectsByHealth.FAIL.map((p) => (
                      <Typography
                        key={p.id}
                        variant="caption"
                        sx={{
                          display: 'block',
                          color: '#00B8D4',
                          cursor: 'pointer',
                          '&:hover': { textDecoration: 'underline' },
                        }}
                        onClick={() => navigate(`/projects/${p.id}`)}
                      >
                        {p.name}
                      </Typography>
                    ))}
                  </Box>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Top Findings Across All Projects
          </Typography>
          <Box>
            {projects.length > 0 && projects.some((p) => (p.stats?.failCount ?? 0) > 0) ? (
              <Box>
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    Total failing components: {projects.reduce((sum, p) => sum + (p.stats?.failCount ?? 0), 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Review project details to identify specific failing rules and remediation steps.
                  </Typography>
                </Box>
              </Box>
            ) : (
              <Typography color="text.secondary">No failing components across all projects.</Typography>
            )}
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}
