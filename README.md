# RLC Index
This repository provides the RLC index, a reachability index for processing graph queries with a concatenation of edge labels under the Kleene operator.

#### Getting Started
1. Clone the project.
2. Execute `mvn clean package`

#### Organization

[core](https://github.com/g-rpqs/rlc-index/tree/main/core) is an implementation of the RLC index.

[demo](https://github.com/g-rpqs/rlc-index/tree/main/demo) shows a simple demonstration of building and querying the RLC index.

[benchmark](https://github.com/g-rpqs/rlc-index/tree/main/benchmark) contains the source code for the experiments in the technical report.

[paper](https://github.com/g-rpqs/rlc-index/tree/main/paper/technical-report.pdf) includes the technical report of the RLC index.

#### Reproducibility
In the rest of this section, suppose that we are in `benchmark/`

1. Download the [rlc-benchmarks.tar.gz](https://drive.google.com/file/d/1cEmnJVipATISRY-QvulQNc2YT13q8-oC/view?usp=sharing) file contains the datasets and workloads used in the experiments of the technical report.

2. Execute `tar -czvf rlc-benchmarks.tar.gz`

3. `benchmark/benchmarks` contains the datasets and workloads. See the technical report for their details.

4. [configurations](https://github.com/g-rpqs/rlc-index/tree/main/benchmark/configurations) contains JSON files for configuring the performance test. The default configuration skips the larger datasets. To include them in the test, remove the corresponding values under 'skippedGraphNames' in JSON files. Before doing this, please ensure that there is enough memory, e.g., at least 32 GB for the dataset '08-soc-pokec-relationships', '09-wiki-topcats', and '10-wiki-Talk', or at least 80 GB for the dataset '11-sx-stackoverflow', '12-soc-LiveJournal1-50', and '13-wikipedia_link_fr-25'.

5. Execute `nohup java -Xms16g -Xmx16g -cp target/benchmark-1.0-SNAPSHOT.jar org.jrpq.rlci.benchamrk.App &` to execute the default performance test. Increase the memory when the larger datasets are included.

6. The benchmark results are available under the directory `benchmark/benchmark-results`.
