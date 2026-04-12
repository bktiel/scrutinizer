import { Box, Typography, Select, MenuItem, Slider, Stack } from '@mui/material'
import type { PolicyForm } from '../../utils/policyYaml'
import { SCORING_METHOD_DESCRIPTIONS } from '../../utils/policyYaml'

const METHOD_LABELS: Record<string, string> = {
  WEIGHTED_AVERAGE: 'Weighted Average',
  PASS_FAIL: 'Pass / Fail',
  WORST_CASE: 'Worst Case',
}

interface Props {
  form: PolicyForm
  onChange: (form: PolicyForm) => void
}

export default function ScoringStep({ form, onChange }: Props) {
  const { scoring } = form
  const warn = scoring.warnThreshold
  const pass = scoring.passThreshold

  const handleSliderChange = (_: Event, value: number | number[]) => {
    if (Array.isArray(value)) {
      onChange({
        ...form,
        scoring: {
          ...scoring,
          warnThreshold: Math.round(value[0] * 10) / 10,
          passThreshold: Math.round(value[1] * 10) / 10,
        },
      })
    }
  }

  // Compute percentage positions for background gradient
  const warnPct = (warn / 10) * 100
  const passPct = (pass / 10) * 100

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Set Scoring
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Choose how rule results combine into an overall score, and set the thresholds for pass, warn, and fail.
      </Typography>

      {/* Scoring Method */}
      <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>
        Scoring Method
      </Typography>
      <Select
        value={scoring.method}
        onChange={(e) =>
          onChange({
            ...form,
            scoring: { ...scoring, method: e.target.value as PolicyForm['scoring']['method'] },
          })
        }
        fullWidth
        size="small"
        sx={{ mb: 0.5 }}
      >
        {Object.entries(METHOD_LABELS).map(([key, label]) => (
          <MenuItem key={key} value={key}>
            {label}
          </MenuItem>
        ))}
      </Select>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 4 }}>
        {SCORING_METHOD_DESCRIPTIONS[scoring.method]}
      </Typography>

      {/* Visual Threshold Slider */}
      <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 600 }}>
        Score Thresholds
      </Typography>

      {/* Colored zone bar */}
      <Box
        sx={{
          height: 12,
          borderRadius: 6,
          mb: 1,
          background: `linear-gradient(to right,
            #FF5252 0%, #FF5252 ${warnPct}%,
            #FFAB00 ${warnPct}%, #FFAB00 ${passPct}%,
            #00E676 ${passPct}%, #00E676 100%)`,
          opacity: 0.7,
        }}
      />

      {/* MUI Slider with two handles */}
      <Slider
        value={[warn, pass]}
        onChange={handleSliderChange}
        min={0}
        max={10}
        step={0.5}
        valueLabelDisplay="on"
        disableSwap
        sx={{
          mt: -1.5,
          '& .MuiSlider-thumb': {
            bgcolor: '#fff',
            border: '2px solid currentColor',
            width: 20,
            height: 20,
          },
          '& .MuiSlider-track': { opacity: 0 },
          '& .MuiSlider-rail': { opacity: 0 },
          '& .MuiSlider-valueLabel': {
            fontSize: '0.75rem',
            bgcolor: 'rgba(0,0,0,0.8)',
          },
        }}
      />

      {/* Zone labels */}
      <Stack direction="row" justifyContent="space-between" sx={{ mt: 1 }}>
        <Box sx={{ textAlign: 'center', flex: warnPct || 1 }}>
          <Typography variant="caption" sx={{ color: '#FF5252', fontWeight: 700 }}>
            FAIL
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            0 &ndash; {warn}
          </Typography>
        </Box>
        <Box sx={{ textAlign: 'center', flex: (passPct - warnPct) || 1 }}>
          <Typography variant="caption" sx={{ color: '#FFAB00', fontWeight: 700 }}>
            WARN
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            {warn} &ndash; {pass}
          </Typography>
        </Box>
        <Box sx={{ textAlign: 'center', flex: (100 - passPct) || 1 }}>
          <Typography variant="caption" sx={{ color: '#00E676', fontWeight: 700 }}>
            PASS
          </Typography>
          <Typography variant="caption" color="text.secondary" display="block">
            {pass} &ndash; 10
          </Typography>
        </Box>
      </Stack>
    </Box>
  )
}
