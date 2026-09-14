FROM python:3.12-alpine
WORKDIR /site
COPY *.html *.css /site/
EXPOSE 80
CMD ["python3", "-m", "http.server", "80"]
