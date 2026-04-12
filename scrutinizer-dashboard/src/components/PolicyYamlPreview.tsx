import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  IconButton,
  Tooltip,
} from '@mui/material'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'

interface PolicyYamlPreviewProps {
  open: boolean
  yaml: string
  onClose: () => void
}

export default function PolicyYamlPreview({
  open,
  yaml,
  onClose,
}: PolicyYamlPreviewProps) {
  const handleCopy = () => {
    navigator.clipboard.writeText(yaml)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          Policy YAML Preview
          <Tooltip title="Copy to clipboard">
            <IconButton size="small" onClick={handleCopy}>
              <ContentCopyIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box
          component="pre"
          sx={{
            bgcolor: '#0A0E14',
            border: '1px solid rgba(139, 148, 158, 0.2)',
            borderRadius: 1,
            p: 2,
            overflow: 'auto',
            maxHeight: 500,
            fontFamily: '"Courier New", monospace',
            fontSize: '0.875rem',
            lineHeight: 1.6,
            color: '#00B8D4',
            whiteSpace: 'pre-wrap',
            wordWrap: 'break-word',
          }}
        >
          {yaml}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}
