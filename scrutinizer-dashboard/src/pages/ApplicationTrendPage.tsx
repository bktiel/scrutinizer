import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Typography, Box, Button } from '@mui/material'
import PostureTrendChart from '../components/PostureTrendChart'
import { getTrends, TrendDataPoint } from '../api/scrutinizerApi'

export default function ApplicationTrendPage() {
  const { applicationName } = useParams<{ applicationName: string }>()
  const navigate = useNavigate()
  const [trends, setTrends] = useState<TrendDataPoint[]>([])

  useEffect(() => {
    if (applicationName) {
      getTrends(applicationName).then(setTrends).catch(console.error)
    }
  }, [applicationName])

  return (
    <Box>
      <Button onClick={() => navigate('/')} sx={{ mb: 2 }}>
        &larr; Back to Dashboard
      </Button>

      <Typography variant="h4" gutterBottom>
        Posture Trend: {applicationName}
      </Typography>

      {trends.length > 0 ? (
        <PostureTrendChart data={trends} />
      ) : (
        <Typography color="text.secondary">No trend data available yet.</Typography>
      )}
    </Box>
  )
}
