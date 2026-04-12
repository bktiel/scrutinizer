import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Box, Button, Stack, Typography, Alert, CircularProgress } from '@mui/material'
import PolicyWizard from '../components/wizard/PolicyWizard'
import { getPolicy, createPolicyFromYaml, updatePolicyFromYaml } from '../api/scrutinizerApi'
import { PolicyForm, INITIAL_FORM, generatePolicyYaml, parseYamlToForm } from '../utils/policyYaml'

export default function PolicyConfiguratorPage() {
  const { id } = useParams<{ id?: string }>()
  const navigate = useNavigate()
  const isEditing = id && id !== 'new'

  const [initialForm, setInitialForm] = useState<PolicyForm>(INITIAL_FORM)
  const [loading, setLoading] = useState(!!isEditing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Load existing policy if editing
  useEffect(() => {
    if (isEditing) {
      getPolicy(id!)
        .then((policy) => {
          const parsed = parseYamlToForm(policy.policyYaml)
          if (parsed) {
            setInitialForm(parsed)
          } else {
            setError('Failed to parse policy YAML')
          }
        })
        .catch((e) => setError(e.message))
        .finally(() => setLoading(false))
    }
  }, [id, isEditing])

  const handleSave = async (form: PolicyForm) => {
    if (!form.name.trim()) {
      setError('Policy name is required')
      return
    }
    if (form.rules.length === 0) {
      setError('At least one rule is required')
      return
    }

    try {
      setSaving(true)
      setError(null)
      const policyYaml = generatePolicyYaml(form)

      if (isEditing) {
        await updatePolicyFromYaml(id!, policyYaml, form.description || undefined)
      } else {
        await createPolicyFromYaml(policyYaml, form.description || undefined)
      }

      navigate('/policies')
    } catch (e: any) {
      setError(e.message || 'Failed to save policy')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '50vh' }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h4">{isEditing ? 'Edit Policy' : 'Create Policy'}</Typography>
        <Button variant="text" onClick={() => navigate('/policies')}>
          Back to Policies
        </Button>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <PolicyWizard
        initialForm={initialForm}
        isEditing={!!isEditing}
        saving={saving}
        onSave={handleSave}
      />
    </Box>
  )
}
