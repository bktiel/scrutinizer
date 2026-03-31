import { DataGrid, GridColDef, GridRenderCellParams } from '@mui/x-data-grid'
import { Chip } from '@mui/material'
import SignalBadge from './SignalBadge'
import type { ComponentResult } from '../api/scrutinizerApi'

interface DependencyTableProps {
  components: ComponentResult[]
}

const columns: GridColDef[] = [
  { field: 'componentName', headerName: 'Name', flex: 2 },
  { field: 'componentVersion', headerName: 'Version', width: 120 },
  {
    field: 'isDirect',
    headerName: 'Scope',
    width: 100,
    renderCell: (params: GridRenderCellParams) => (
      <Chip label={params.value ? 'Direct' : 'Transitive'} size="small" variant="outlined" />
    ),
  },
  {
    field: 'decision',
    headerName: 'Decision',
    width: 100,
    renderCell: (params: GridRenderCellParams) => (
      <SignalBadge value={params.value} type="decision" />
    ),
  },
  { field: 'purl', headerName: 'Package URL', flex: 2 },
  {
    field: 'findingCount',
    headerName: 'Findings',
    width: 90,
    valueGetter: (params: { row: ComponentResult }) => params.row.findings?.length ?? 0,
  },
]

export default function DependencyTable({ components }: DependencyTableProps) {
  return (
    <DataGrid
      rows={components}
      columns={columns}
      getRowId={(row) => row.id}
      initialState={{ pagination: { paginationModel: { pageSize: 25 } } }}
      pageSizeOptions={[10, 25, 50]}
      disableRowSelectionOnClick
      autoHeight
      density="compact"
    />
  )
}
