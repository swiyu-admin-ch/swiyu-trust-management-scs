export default {
  collectCoverage: true,
  coverageDirectory: '<rootDir>/coverage/sonarQube',
  collectCoverageFrom: ['<rootDir>/src/**/*.ts', '!<rootDir>/src/app/api/generated/**'],
  testResultsProcessor: 'jest-sonar-reporter'
};
