import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { Typography, Box } from '@mui/material'
import SignalBadge from '../components/SignalBadge'
import { listRuns, PostureRunSummary } from '../api/scrutinizerApi'

const columns: GridColDef[] = [
  { field: 'applicationName', headerName: 'Application', flex: 1 },
  { field: 'policyName', headerName: 'Policy', flex: 1 },
  { field: 'policyVersion', headerName: 'Version', width: 80 },
  {
    field: 'overallDecision',
    headerName: 'Decision',
    width: 100,
    renderCell: (params: GridRenderCellParams) => (
      <SignalBadge value={params.value} type="decision" />
    ),
  },
  {
    field: 'postureScore',
    headerName: 'Score',
    width: 80,
    renderCell: (params: GridRenderCellParams) => (
      <SignalBadge value={params.value} type="score" />
    ),
  },
  {
    field: 'runTimestamp',
    headerName: 'Evaluated',
    width: 180,
    valueFormatter: (params: { value: string }) => new Date(params.value).toLocaleString(),
  },
]

export default function DashboardPage() {
  const navigate = useNavigate()
  const [runs, setRuns] = useState<PostureRunSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    listRuns(0, 50)
      .then((page) => setRuns(page.content))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Posture Evaluation Runs
      </Typography>
      <DataGrid
        rows={runs}
        columns={columns}
        getRowId={(row) => row.id}
        loading={loading}
        onRowClick={(params) => navigate(`/runs/${params.id}`)}
        initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        pageSizeOptions={[10, 25, 50]}
        disableRowSelectionOnClick
        autoHeight
        sx={{ cursor: 'pointer' }}
      />
    </Box>
  )
}
