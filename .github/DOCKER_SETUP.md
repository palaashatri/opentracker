# Docker Hub Setup for GitHub Actions

1. Create a Docker Hub account at https://hub.docker.com (username: palaasha)
2. Create an access token: Hub → Account Settings → Security → New Access Token
3. In your GitHub repo: Settings → Secrets and variables → Actions → New repository secret:
   - Name: `DOCKER_USERNAME`, Value: `palaasha`
   - Name: `DOCKER_TOKEN`, Value: (your Docker Hub access token)

Images will be pushed to:
- `palaasha/dt-gateway`
- `palaasha/dt-ingestion`
- `palaasha/dt-stream-processor`
- `palaasha/dt-geospatial`
- `palaasha/dt-frontend`
