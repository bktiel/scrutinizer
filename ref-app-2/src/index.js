const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const axios = require('axios');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const moment = require('moment');
const chalk = require('chalk');
const winston = require('winston');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;
const jwtSecret = process.env.JWT_SECRET || 'demo-secret-key';

// Logger setup
const logger = winston.createLogger({
  level: 'info',
  format: winston.format.json(),
  transports: [
    new winston.transports.Console(),
    new winston.transports.File({ filename: 'app.log' })
  ]
});

// Middleware
app.use(helmet());
app.use(cors());
app.use(express.json());

// Middleware to log requests
app.use((req, res, next) => {
  logger.info(`${req.method} ${req.path}`);
  next();
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({
    status: 'healthy',
    timestamp: moment().toISOString(),
    version: '1.0.0'
  });
});

// Demo authentication endpoint
app.post('/auth/register', async (req, res) => {
  try {
    const { username, password } = req.body;
    const hashedPassword = await bcrypt.hash(password, 10);
    const userId = uuidv4();

    res.json({
      userId,
      username,
      message: 'User registered successfully'
    });
  } catch (error) {
    logger.error('Registration error:', error);
    res.status(500).json({ error: 'Registration failed' });
  }
});

// Token generation endpoint
app.post('/auth/token', (req, res) => {
  try {
    const token = jwt.sign(
      { userId: uuidv4(), exp: Math.floor(Date.now() / 1000) + 3600 },
      jwtSecret
    );

    res.json({ token });
  } catch (error) {
    logger.error('Token generation error:', error);
    res.status(500).json({ error: 'Token generation failed' });
  }
});

// Demo API call endpoint
app.get('/api/external', async (req, res) => {
  try {
    const response = await axios.get('https://httpbin.org/get', {
      headers: { 'User-Agent': 'Scrutinizer/1.0' }
    });

    res.json({
      data: response.data,
      timestamp: moment().toISOString()
    });
  } catch (error) {
    logger.error('External API call error:', error);
    res.status(500).json({ error: 'External API call failed' });
  }
});

// Start server
app.listen(port, () => {
  console.log(chalk.green(`Server running on port ${port}`));
  console.log(chalk.blue(`Health check: http://localhost:${port}/health`));
  logger.info(`Application started on port ${port}`);
});

module.exports = app;
