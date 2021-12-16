FROM rust:1.57 as build
WORKDIR /code
COPY src /code/src
COPY Cargo.toml /code/Cargo.toml
RUN cargo build --release

FROM registry.access.redhat.com/ubi8/ubi-minimal:8.5
WORKDIR /work/
COPY --from=build /code/target/release/weather-batch-rust /work/application
# set up permissions for user `1001`
RUN chmod 775 /work /work/application \
  && chown -R 1001 /work \
  && chmod -R "g+rwX" /work \
  && chown -R 1001:root /work

USER 1001
CMD ["./application"]
