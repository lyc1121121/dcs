FROM python:3.12-alpine
WORKDIR /site
COPY *.html *.css /site/
COPY documentation/ /site/documentation/
EXPOSE 80
CMD ["python3", "-m", "http.server", "80"]
