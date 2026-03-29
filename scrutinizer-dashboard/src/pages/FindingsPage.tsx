import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { Typography, Box, Button, FormControl, InputLabel, Select, MenuItem } from '@mui/material'
import SignalBadge from '../components/SignalBadge'
import { getRunFindings, Finding } from '../api/scrutinizerApi'

const columns: GridColDef[] = [
  { field: 'componentName', headerName: 'Component', flex: 1 },
  { field: 'ruleId', headerName: 'Rule', width: 180 },
  {
    field: 'decision',
    headerName: 'Decision',
    width: 100,
    renderCell: (params: GridRenderCellParams) => (
      <SignalBadge value={params.value} type="decision" />
    ),
  },
  { field: 'severity', headerName: 'Severity', width: 90 },
  { field: 'field', headerName: 'Field', width: 160 },
  { field: 'actualValue', headerName: 'Actual', width: 120 },
  { field: 'expectedValue', headerName: 'Expected', width: 120 },
  { field: 'description', headerName: 'Description', flex: 1 },
  { field: 'remediation', headerName: 'Remediation', flex: 1 },
]

export default function FindingsPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [findings, setFindings] = useState<Finding[]>([])
  const [loading, setLoading] = useState(true)
  const [decisionFilter, setDecisionFilter] = useState('')

  useEffect(() => {
    if (id) {
      setLoading(true)
      getRunFindings(id, 0, 200, decisionFilter || undefined)
        .then((page) => setFindings(page.content))
        .catch(console.error)
        .finally(() => setLoading(false))
    }
  }, [id, decisionFilter])

  return (
    <Box>
      <Button onClick={() => navigate(`/runs/${id}`)} sx={{ mb: 2 }}>
        &larr; Back to Run Detail
      </Button>

      <Typography variant="h4" gutterBottom>
        Findings
      </Typography>

      <FormControl sx={{ mb: 2, minWidth: 150 }} size="small">
        <InputLabel>Decision</InputLabel>
        <Select
          value={decisionFilter}
          label="Decision"
          onChange={(e) => setDecisionFilter(e.target.value)}
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="FAIL">Fail</MenuItem>
          <MenuItem value="WARN">Warn</MenuItem>
          <MenuItem value="INFO">Info</MenuItem>
        </Select>
      </FormControl>

      <DataGrid
        rows={findings}
        columns={columns}
        getRowId={(row) => row.id}
        loading={loading}
        initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
        pageSizeOptions={[10, 25, 50]}
        disableRowSelectionOnClick
        autoHeight
        density="compact"
      />
    </Box>
  )
}
