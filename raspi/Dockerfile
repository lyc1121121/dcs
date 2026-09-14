FROM python:3.12-alpine
WORKDIR /site
COPY index.html /site/index.html
EXPOSE 80
CMD ["python3", "-m", "http.server", "80"]
