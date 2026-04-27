import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Typography, Box, Tabs, Tab, Paper, Grid, Button, Tooltip } from '@mui/material'
import DownloadIcon from '@mui/icons-material/Download'
import SignalBadge from '../components/SignalBadge'
import DependencyTable from '../components/DependencyTable'
import { getRunDetail, PostureRunDetail } from '../api/scrutinizerApi'

export default function RunDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [run, setRun] = useState<PostureRunDetail | null>(null)
  const [tab, setTab] = useState(0)

  useEffect(() => {
    if (id) {
      getRunDetail(id).then(setRun).catch(console.error)
    }
  }, [id])

  if (!run) return <Typography>Loading...</Typography>

  const summary = run.summaryJson ? JSON.parse(run.summaryJson) : {}

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Button onClick={() => navigate('/')}>&larr; Back to Dashboard</Button>
        <Tooltip title="Download audit bundle (posture report, findings CSV, evidence manifest)">
          <Button
            variant="outlined"
            size="small"
            startIcon={<DownloadIcon />}
            onClick={() => {
              const link = document.createElement('a')
              link.href = `/api/v1/runs/${id}/export`
              link.download = ''
              link.click()
            }}
          >
            Export Audit Bundle
          </Button>
        </Tooltip>
      </Box>

      <Typography variant="h4" gutterBottom>
        {run.applicationName}
      </Typography>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Grid container spacing={2}>
          <Grid item xs={3}>
            <Typography variant="subtitle2" color="text.secondary">Decision</Typography>
            <SignalBadge value={run.overallDecision} type="decision" />
          </Grid>
          <Grid item xs={3}>
            <Typography variant="subtitle2" color="text.secondary">Score</Typography>
            <SignalBadge value={run.postureScore} type="score" />
          </Grid>
          <Grid item xs={3}>
            <Typography variant="subtitle2" color="text.secondary">Policy</Typography>
            <Typography>{run.policyName} v{run.policyVersion}</Typography>
          </Grid>
          <Grid item xs={3}>
            <Typography variant="subtitle2" color="text.secondary">Evaluated</Typography>
            <Typography>{new Date(run.runTimestamp).toLocaleString()}</Typography>
          </Grid>
        </Grid>

        {summary.total !== undefined && (
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Rules: {summary.total} total | {summary.pass} pass | {summary.warn} warn | {summary.fail} fail | {summary.info} info | {summary.skip} skip
            </Typography>
          </Box>
        )}
      </Paper>

      <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mb: 2 }}>
        <Tab label="Dependencies" />
        <Tab label="Findings" />
      </Tabs>

      {tab === 0 && <DependencyTable components={run.componentResults} />}
      {tab === 1 && (
        <Button variant="outlined" onClick={() => navigate(`/runs/${id}/findings`)}>
          View All Findings
        </Button>
      )}
    </Box>
  )
}
