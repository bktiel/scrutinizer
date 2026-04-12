import { useState } from 'react'
import {
  Box,
  Stepper,
  Step,
  StepLabel,
  Button,
  Stack,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import type { PolicyForm } from '../../utils/policyYaml'
import { parseYamlToForm } from '../../utils/policyYaml'
import type { PolicyTemplate } from '../../data/policyTemplates'
import TemplateSelectionStep from './TemplateSelectionStep'
import MetadataStep from './MetadataStep'
import RulesStep from './RulesStep'
import ScoringStep from './ScoringStep'
import ReviewStep from './ReviewStep'

const STEPS = ['Choose Template', 'Policy Details', 'Define Rules', 'Set Scoring', 'Review & Save']

interface Props {
  initialForm: PolicyForm
  isEditing: boolean
  saving: boolean
  onSave: (form: PolicyForm) => void
}

export default function PolicyWizard({ initialForm, isEditing, saving, onSave }: Props) {
  const [form, setForm] = useState<PolicyForm>(initialForm)
  const [activeStep, setActiveStep] = useState(isEditing ? 1 : 0)
  const [stepError, setStepError] = useState<string | null>(null)

  // Import YAML dialog state
  const [importOpen, setImportOpen] = useState(false)
  const [importYaml, setImportYaml] = useState('')
  const [importError, setImportError] = useState<string | null>(null)

  const handleTemplateSelect = (template: PolicyTemplate) => {
    setForm({ ...template.form })
    setStepError(null)
    setActiveStep(1)
  }

  const validateStep = (step: number): boolean => {
    setStepError(null)
    if (step === 1 && !form.name.trim()) {
      setStepError('Policy name is required.')
      return false
    }
    if (step === 2 && form.rules.length === 0) {
      setStepError('Add at least one rule before continuing.')
      return false
    }
    return true
  }

  const handleNext = () => {
    if (validateStep(activeStep)) {
      setActiveStep((prev) => prev + 1)
    }
  }

  const handleBack = () => {
    setStepError(null)
    setActiveStep((prev) => prev - 1)
  }

  const handleSave = () => {
    if (!form.name.trim()) {
      setStepError('Policy name is required.')
      return
    }
    if (form.rules.length === 0) {
      setStepError('At least one rule is required.')
      return
    }
    onSave(form)
  }

  const handleImport = () => {
    setImportError(null)
    const parsed = parseYamlToForm(importYaml)
    if (parsed) {
      setForm(parsed)
      setImportOpen(false)
      setImportYaml('')
      setActiveStep(1)
    } else {
      setImportError('Invalid YAML format. Make sure it follows the Scrutinizer policy schema.')
    }
  }

  const renderStep = () => {
    switch (activeStep) {
      case 0:
        return <TemplateSelectionStep onSelect={handleTemplateSelect} />
      case 1:
        return <MetadataStep form={form} onChange={setForm} />
      case 2:
        return <RulesStep form={form} onChange={setForm} />
      case 3:
        return <ScoringStep form={form} onChange={setForm} />
      case 4:
        return <ReviewStep form={form} />
      default:
        return null
    }
  }

  return (
    <Box>
      {/* Stepper */}
      <Stepper
        activeStep={activeStep}
        alternativeLabel
        sx={{
          mb: 4,
          '& .MuiStepLabel-label': { fontSize: '0.8rem' },
          '& .MuiStepIcon-root.Mui-active': { color: '#00B8D4' },
          '& .MuiStepIcon-root.Mui-completed': { color: '#00B8D4' },
        }}
      >
        {STEPS.map((label, index) => (
          <Step key={label} completed={index < activeStep}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>

      {/* Step Error */}
      {stepError && (
        <Alert severity="warning" sx={{ mb: 2 }} onClose={() => setStepError(null)}>
          {stepError}
        </Alert>
      )}

      {/* Active Step Content */}
      <Box sx={{ minHeight: 300, mb: 4 }}>{renderStep()}</Box>

      {/* Navigation */}
      {activeStep > 0 && (
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <Stack direction="row" spacing={1}>
            <Button onClick={handleBack} variant="text">
              Back
            </Button>
            <Button
              variant="text"
              startIcon={<EditIcon />}
              onClick={() => setImportOpen(true)}
              sx={{ color: 'text.secondary' }}
            >
              Import YAML
            </Button>
          </Stack>

          {activeStep < STEPS.length - 1 ? (
            <Button variant="contained" onClick={handleNext}>
              Next
            </Button>
          ) : (
            <Button variant="contained" onClick={handleSave} disabled={saving}>
              {saving ? <CircularProgress size={22} /> : isEditing ? 'Update Policy' : 'Create Policy'}
            </Button>
          )}
        </Stack>
      )}

      {/* Import YAML Dialog */}
      <Dialog open={importOpen} onClose={() => setImportOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Import Policy YAML</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Policy YAML"
              value={importYaml}
              onChange={(e) => setImportYaml(e.target.value)}
              fullWidth
              multiline
              rows={12}
              placeholder="Paste your policy YAML here..."
              helperText="Paste a complete Scrutinizer policy YAML to populate the wizard."
            />
            {importError && <Alert severity="error">{importError}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleImport} disabled={!importYaml.trim()}>
            Import
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
