import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
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
  Chip,
  Stack,
  CircularProgress,
  Paper,
  FormControlLabel,
  RadioGroup,
  Radio,
} from '@mui/material'
import AddIcon from '@mui/icons-material/Add'
import DeleteIcon from '@mui/icons-material/Delete'
import BlockIcon from '@mui/icons-material/Block'
import {
  listExceptions,
  createException,
  updateException,
  deleteException,
  listProjects,
  listPolicies,
  PolicyException,
  Project,
  Policy,
} from '../api/scrutinizerApi'

const STATUSES = ['ALL', 'ACTIVE', 'EXPIRED', 'REVOKED']
const SCOPES = ['ALL', 'PROJECT', 'GLOBAL']

const getStatusColor = (status: string): 'success' | 'warning' | 'error' | 'default' => {
  switch (status?.toUpperCase()) {
    case 'ACTIVE':
      return 'success'
    case 'EXPIRED':
      return 'warning'
    case 'REVOKED':
      return 'error'
    default:
      return 'default'
  }
}

const getScopeColor = (scope: string): 'primary' | 'secondary' | 'default' => {
  return scope?.toUpperCase() === 'GLOBAL' ? 'primary' : 'secondary'
}

export default function ExceptionsPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [exceptions, setExceptions] = useState<PolicyException[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [policies, setPolicies] = useState<Policy[]>([])
  const [loading, setLoading] = useState(true)
  const [dialogOpen, setDialogOpen] = useState(false)

  const [statusFilter, setStatusFilter] = useState('ALL')
  const [scopeFilter, setScopeFilter] = useState('ALL')
  const [searchText, setSearchText] = useState('')

  const [formData, setFormData] = useState({
    projectId: '',
    policyId: '',
    ruleId: '',
    packageName: '',
    packageVersion: '',
    justification: '',
    scope: 'PROJECT',
    expiresAt: '',
    createdBy: 'system',
  })

  // Load data on mount
  useEffect(() => {
    Promise.all([listExceptions(), listProjects(), listPolicies()])
      .then(([exceptionsData, projectsData, policiesData]) => {
        setExceptions(exceptionsData)
        setProjects(projectsData)
        setPolicies(policiesData)

        // Pre-populate from URL params
        const projectId = searchParams.get('projectId')
        const ruleId = searchParams.get('ruleId')
        const packageName = searchParams.get('packageName')

        if (projectId || ruleId || packageName) {
          setDialogOpen(true)
          setFormData((prev) => ({
            ...prev,
            projectId: projectId || '',
            ruleId: ruleId || '',
            packageName: packageName || '',
          }))
        }
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [searchParams])

  const handleCreateException = async () => {
    if (!formData.projectId.trim() || !formData.justification.trim()) {
      return
    }
    try {
      const newException = await createException({
        projectId: formData.projectId,
        policyId: formData.policyId || undefined,
        ruleId: formData.ruleId || undefined,
        packageName: formData.packageName || undefined,
        packageVersion: formData.packageVersion || undefined,
        justification: formData.justification,
        scope: formData.scope,
        expiresAt: formData.expiresAt || undefined,
      })
      setExceptions([newException, ...exceptions])
      setDialogOpen(false)
      setFormData({
        projectId: '',
        policyId: '',
        ruleId: '',
        packageName: '',
        packageVersion: '',
        justification: '',
        scope: 'PROJECT',
        expiresAt: '',
        createdBy: 'system',
      })
    } catch (error) {
      console.error('Failed to create exception:', error)
    }
  }

  const handleRevokeException = async (id: string) => {
    try {
      const updated = await updateException(id, { status: 'REVOKED' })
      setExceptions(exceptions.map((e) => (e.id === id ? updated : e)))
    } catch (error) {
      console.error('Failed to revoke exception:', error)
    }
  }

  const handleDeleteException = async (id: string) => {
    if (!confirm('Are you sure you want to delete this exception?')) return
    try {
      await deleteException(id)
      setExceptions(exceptions.filter((e) => e.id !== id))
    } catch (error) {
      console.error('Failed to delete exception:', error)
    }
  }

  // Filter exceptions
  const filteredExceptions = exceptions.filter((exc) => {
    const matchesStatus = statusFilter === 'ALL' || exc.status?.toUpperCase() === statusFilter
    const matchesScope = scopeFilter === 'ALL' || exc.scope?.toUpperCase() === scopeFilter
    const matchesSearch =
      searchText === '' ||
      exc.packageName?.toLowerCase().includes(searchText.toLowerCase()) ||
      exc.ruleId?.toLowerCase().includes(searchText.toLowerCase())
    return matchesStatus && matchesScope && matchesSearch
  })

  const getProjectName = (projectId: string): string => {
    return projects.find((p) => p.id === projectId)?.name || 'Unknown Project'
  }

  const formatExpiry = (expiresAt: string | null): string => {
    if (!expiresAt) return 'Permanent'
    const expiryDate = new Date(expiresAt)
    const now = new Date()
    const daysLeft = Math.ceil((expiryDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))

    if (daysLeft < 0) return 'Expired'
    if (daysLeft === 0) return 'Expires today'
    if (daysLeft === 1) return 'Expires tomorrow'
    if (daysLeft <= 7) return `${daysLeft} days left`

    return expiryDate.toLocaleDateString()
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
        <Typography variant="h4">Policy Exceptions</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => {
            setFormData({
              projectId: '',
              policyId: '',
              ruleId: '',
              packageName: '',
              packageVersion: '',
              justification: '',
              scope: 'PROJECT',
              expiresAt: '',
              createdBy: 'system',
            })
            setDialogOpen(true)
          }}
        >
          Create Exception
        </Button>
      </Box>

      {/* Filter bar */}
      <Paper sx={{ p: 2, mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Typography variant="caption" sx={{ fontWeight: 600 }}>
            Status:
          </Typography>
          {STATUSES.map((status) => (
            <Chip
              key={status}
              label={status}
              onClick={() => setStatusFilter(status)}
              variant={statusFilter === status ? 'filled' : 'outlined'}
              color={statusFilter === status ? 'primary' : 'default'}
              size="small"
            />
          ))}
        </Box>

        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Typography variant="caption" sx={{ fontWeight: 600 }}>
            Scope:
          </Typography>
          {SCOPES.map((scope) => (
            <Chip
              key={scope}
              label={scope}
              onClick={() => setScopeFilter(scope)}
              variant={scopeFilter === scope ? 'filled' : 'outlined'}
              color={scopeFilter === scope ? 'primary' : 'default'}
              size="small"
            />
          ))}
        </Box>

        <TextField
          placeholder="Search by package name or rule ID..."
          size="small"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          sx={{ flex: 1, minWidth: 250 }}
        />
      </Paper>

      {/* Exception cards */}
      <Grid container spacing={2}>
        {filteredExceptions.map((exc) => (
          <Grid item xs={12} key={exc.id}>
            <Card
              sx={{
                display: 'flex',
                flexDirection: 'column',
                borderLeft: `4px solid ${
                  exc.status?.toUpperCase() === 'ACTIVE'
                    ? '#00E676'
                    : exc.status?.toUpperCase() === 'EXPIRED'
                    ? '#FFAB00'
                    : '#FF5252'
                }`,
              }}
            >
              <CardContent>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={8}>
                    <Box sx={{ mb: 2 }}>
                      <Stack direction="row" spacing={1} sx={{ mb: 1 }}>
                        <Chip
                          label={exc.status?.toUpperCase() || 'UNKNOWN'}
                          size="small"
                          color={getStatusColor(exc.status)}
                          variant="filled"
                        />
                        <Chip
                          label={exc.scope?.toUpperCase() || 'PROJECT'}
                          size="small"
                          color={getScopeColor(exc.scope)}
                          variant="outlined"
                        />
                      </Stack>
                    </Box>

                    {/* Package info */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="caption" color="text.secondary">
                        Package
                      </Typography>
                      <Typography variant="body2">
                        {exc.packageName ? (
                          <>
                            {exc.packageName}
                            {exc.packageVersion && ` @ ${exc.packageVersion}`}
                          </>
                        ) : (
                          'All packages'
                        )}
                      </Typography>
                    </Box>

                    {/* Rule info */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="caption" color="text.secondary">
                        Rule
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {exc.ruleId || 'All rules'}
                      </Typography>
                    </Box>

                    {/* Justification */}
                    <Box sx={{ mb: 2 }}>
                      <Typography variant="caption" color="text.secondary">
                        Justification
                      </Typography>
                      <Typography variant="body2" sx={{ mt: 0.5, maxWidth: 500 }}>
                        {exc.justification}
                      </Typography>
                    </Box>

                    {/* Created info */}
                    <Box sx={{ display: 'flex', gap: 4, mb: 2 }}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Created By
                        </Typography>
                        <Typography variant="caption" sx={{ display: 'block' }}>
                          {exc.createdBy}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Created
                        </Typography>
                        <Typography variant="caption" sx={{ display: 'block' }}>
                          {new Date(exc.createdAt).toLocaleDateString()}
                        </Typography>
                      </Box>
                      {exc.approvedBy && (
                        <Box>
                          <Typography variant="caption" color="text.secondary">
                            Approved By
                          </Typography>
                          <Typography variant="caption" sx={{ display: 'block' }}>
                            {exc.approvedBy}
                          </Typography>
                        </Box>
                      )}
                    </Box>
                  </Grid>

                  <Grid item xs={12} sm={4} sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                    {/* Project */}
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Project
                      </Typography>
                      <Typography
                        variant="body2"
                        sx={{
                          color: '#00B8D4',
                          cursor: 'pointer',
                          '&:hover': { textDecoration: 'underline' },
                        }}
                        onClick={() => navigate(`/projects/${exc.projectId}`)}
                      >
                        {getProjectName(exc.projectId)}
                      </Typography>
                    </Box>

                    {/* Expires */}
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Expiration
                      </Typography>
                      <Typography variant="body2">
                        {formatExpiry(exc.expiresAt)}
                      </Typography>
                    </Box>

                    {/* Exception ID */}
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        ID
                      </Typography>
                      <Typography variant="caption" sx={{ fontFamily: 'monospace', display: 'block' }}>
                        {exc.id.substring(0, 8)}...
                      </Typography>
                    </Box>
                  </Grid>
                </Grid>
              </CardContent>

              <CardActions sx={{ justifyContent: 'flex-end', gap: 1 }}>
                {exc.status?.toUpperCase() === 'ACTIVE' && (
                  <Button
                    size="small"
                    startIcon={<BlockIcon />}
                    onClick={() => handleRevokeException(exc.id)}
                  >
                    Revoke
                  </Button>
                )}
                <Button
                  size="small"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={() => handleDeleteException(exc.id)}
                >
                  Delete
                </Button>
              </CardActions>
            </Card>
          </Grid>
        ))}
      </Grid>

      {filteredExceptions.length === 0 && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {searchText || statusFilter !== 'ALL' || scopeFilter !== 'ALL'
              ? 'No exceptions match your filters.'
              : 'No policy exceptions yet.'}
          </Typography>
        </Paper>
      )}

      {/* Create Exception Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <BlockIcon sx={{ color: '#00B8D4' }} />
          Create Policy Exception
        </DialogTitle>
        <DialogContent sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
          {/* Project selector */}
          <FormControl fullWidth required>
            <InputLabel>Project</InputLabel>
            <Select
              label="Project"
              value={formData.projectId}
              onChange={(e) => setFormData({ ...formData, projectId: e.target.value })}
            >
              <MenuItem value="">Select a project</MenuItem>
              {projects.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Policy selector */}
          <FormControl fullWidth>
            <InputLabel>Policy (Optional)</InputLabel>
            <Select
              label="Policy (Optional)"
              value={formData.policyId}
              onChange={(e) => setFormData({ ...formData, policyId: e.target.value })}
            >
              <MenuItem value="">All policies</MenuItem>
              {policies.map((p) => (
                <MenuItem key={p.id} value={p.id}>
                  {p.name} v{p.version}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Rule ID */}
          <TextField
            label="Rule ID (Optional)"
            fullWidth
            value={formData.ruleId}
            onChange={(e) => setFormData({ ...formData, ruleId: e.target.value })}
            helperText="Leave empty to except all rules"
            size="small"
          />

          {/* Package name */}
          <TextField
            label="Package Name (Optional)"
            fullWidth
            value={formData.packageName}
            onChange={(e) => setFormData({ ...formData, packageName: e.target.value })}
            helperText="Leave empty to except all packages. Use exact package name from SBOM."
            size="small"
          />

          {/* Package version */}
          <TextField
            label="Package Version (Optional)"
            fullWidth
            value={formData.packageVersion}
            onChange={(e) => setFormData({ ...formData, packageVersion: e.target.value })}
            helperText="Leave empty for all versions of the package"
            size="small"
          />

          {/* Justification */}
          <TextField
            label="Justification"
            fullWidth
            multiline
            rows={4}
            required
            value={formData.justification}
            onChange={(e) => setFormData({ ...formData, justification: e.target.value })}
            helperText="Explain why this exception is needed. This will appear in audit records."
          />

          {/* Scope */}
          <FormControl component="fieldset">
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              Scope
            </Typography>
            <RadioGroup
              value={formData.scope}
              onChange={(e) => setFormData({ ...formData, scope: e.target.value })}
              row
            >
              <FormControlLabel
                value="PROJECT"
                control={<Radio />}
                label="PROJECT - applies only to selected project"
              />
              <FormControlLabel
                value="GLOBAL"
                control={<Radio />}
                label="GLOBAL - applies to all projects"
              />
            </RadioGroup>
          </FormControl>

          {/* Expires at */}
          <TextField
            label="Expires At (Optional)"
            type="datetime-local"
            fullWidth
            InputLabelProps={{ shrink: true }}
            value={formData.expiresAt}
            onChange={(e) => setFormData({ ...formData, expiresAt: e.target.value })}
            helperText="When this exception automatically expires. Leave empty for permanent exception."
            size="small"
          />

          {/* Created by */}
          <TextField
            label="Created By"
            fullWidth
            value={formData.createdBy}
            onChange={(e) => setFormData({ ...formData, createdBy: e.target.value })}
            size="small"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleCreateException}
            disabled={!formData.projectId.trim() || !formData.justification.trim()}
          >
            Create Exception
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
