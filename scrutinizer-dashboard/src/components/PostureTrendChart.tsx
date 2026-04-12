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
    if (decision === 'PASS') return '#00E676'
    if (decision === 'WARN') return '#FFAB00'
    return '#FF5252'
  }

  const lastDecision = data.length > 0 ? data[data.length - 1].overallDecision : 'PASS'

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgba(139, 148, 158, 0.1)" />
        <XAxis
          dataKey="date"
          tick={{ fill: '#8B949E' }}
          style={{ fontSize: '0.875rem' }}
        />
        <YAxis
          domain={[0, 10]}
          tick={{ fill: '#8B949E' }}
          style={{ fontSize: '0.875rem' }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: '#1C2333',
            border: '1px solid rgba(139, 148, 158, 0.3)',
            borderRadius: 8,
            color: '#E6EDF3',
          }}
          labelStyle={{ color: '#E6EDF3' }}
        />
        <ReferenceLine y={7} stroke="#00E676" strokeDasharray="5 5" label={{ fill: '#8B949E', fontSize: 12 }} />
        <ReferenceLine y={4} stroke="#FFAB00" strokeDasharray="5 5" label={{ fill: '#8B949E', fontSize: 12 }} />
        <Line
          type="monotone"
          dataKey="postureScore"
          stroke={lineColor(lastDecision)}
          strokeWidth={2}
          dot={{ r: 4, fill: lineColor(lastDecision) }}
          activeDot={{ r: 6, fill: lineColor(lastDecision) }}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
