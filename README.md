# RLC Index
This repository provides the RLC index, a reachability index for processing graph queries with a concatenation of edge labels under the Kleene operator.

For the technical details about the RLC index, please refer to our technical report: https://arxiv.org/abs/2203.08606

#### Organization

[core](https://github.com/g-rpqs/rlc-index/tree/main/core) is an implementation of the RLC index.

[demo](https://github.com/g-rpqs/rlc-index/tree/main/demo) shows a simple demonstration of building and querying the RLC index.

[benchmark](https://github.com/g-rpqs/rlc-index/tree/main/benchmark) contains the source code of the experiments in the technical report.




#### Getting Started
1. Clone the project.
2. Execute `mvn clean package`.


#### Reproducibility
In the rest of this section, suppose that we are in `benchmark/`

1. Download the [rlc-benchmarks.tar.gz](https://drive.google.com/file/d/1cEmnJVipATISRY-QvulQNc2YT13q8-oC/view?usp=sharing) file (2.89GB) contains the datasets and workloads used in the experiments of the technical report.

2. Execute `tar -czvf rlc-benchmarks.tar.gz`.

3. `benchmark/benchmarks` contains the datasets and workloads. Please see the technical report for their details.

4. [configurations](https://github.com/g-rpqs/rlc-index/tree/main/benchmark/configurations) contains JSON files for configuring the performance test. For example, [real-graphs-indexing.json](https://github.com/g-rpqs/rlc-index/blob/main/benchmark/configurations/real-graphs-indexing.json) is used to configure the experiments of indexing time and index size, and [real-graphs-querying.json](https://github.com/g-rpqs/rlc-index/blob/main/benchmark/configurations/real-graphs-querying.json) is for the experiments of querying time. The default parameters in the configuration files skip the large datasets. To include them in the test, remove the corresponding values under the 'skippedGraphNames' entry in JSON files. Before doing this, please ensure that there is enough memory, e.g., at least 32GB for the dataset '08-soc-pokec-relationships', '09-wiki-topcats', and '10-wiki-Talk', or at least 80GB for the dataset '11-sx-stackoverflow', '12-soc-LiveJournal1-50', and '13-wikipedia_link_fr-25'.

5. Execute `nohup java -Xms16g -Xmx16g -cp target/benchmark-1.0-SNAPSHOT.jar org.jrpq.rlci.benchamrk.App &` to start the performance test. Increase the memory when the large datasets are included. Building the extended transitive closure can take around 30 minutes for the smallest dataset in the benchmarks and cannot finish in 24 hours for the other datasets. To skip the extended transitive closure in the test, remove 'Etc' under the 'method' entry in the configuration files.

6. The benchmark results are available under the directory `benchmark/benchmark-results`, which contains sub-directories with results of different tests.
