import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, ReferenceLine } from 'recharts'
import type { TrendDataPoint } from '../api/scrutinizerApi'

interface PostureTrendChartProps {
  data: TrendDataPoint[]
}

export default function PostureTrendChart({ data }: PostureTrendChartProps) {
  const chartData = data.map((d) => ({
    ...d,
    date: new Date(d.timestamp).toLocaleDateString(),
  }))

  const lineColor = (decision: string) => {
    if (decision === 'PASS') return '#2e7d32'
    if (decision === 'WARN') return '#ed6c02'
    return '#d32f2f'
  }

  const lastDecision = data.length > 0 ? data[data.length - 1].overallDecision : 'PASS'

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="date" />
        <YAxis domain={[0, 10]} />
        <Tooltip />
        <ReferenceLine y={7} stroke="#2e7d32" strokeDasharray="5 5" label="Pass" />
        <ReferenceLine y={4} stroke="#ed6c02" strokeDasharray="5 5" label="Warn" />
        <Line
          type="monotone"
          dataKey="postureScore"
          stroke={lineColor(lastDecision)}
          strokeWidth={2}
          dot={{ r: 4 }}
          activeDot={{ r: 6 }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
