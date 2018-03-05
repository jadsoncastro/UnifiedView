function runtime  = runtimeLapSVM(fileName, labeledObjects, knnPar, sigma, options)
	%   runtimeLapSVM calculates the runtime
	%   for the semi-supervised classification algorithm LapSVM.
	%   Author: Jadson Gertrudes
	%   Year  : 2017

	dataSet = csvread(fileName);
	dataSet = dataSet(:, 1:size(dataSet,2)-1);
	labeledObjects = sortrows(labeledObjects, 1);

	n = size(dataSet,1);

	% Compute distance matrix
	distanceMatrix = squareform(pdist(dataSet, 'euclidean'));

	overallTime = tic;
	% Compute the weighted matrix given the mutual knn graph
	WeightMatrix = SimGraph_NearestNeighbors(dataSet', knnPar, 2, sigma);

	% Compute the normalized laplacian
	L = normalizedLaplacian(WeightMatrix);

	% Compute the gram matrix
	GramMatrix = gram(distanceMatrix, sigma);

	%% Information about the class labels
	classIds = unique(labeledObjects(:,2));


	processTime = tic;
%	Temporary variables
	tmpLabeledObjects = [];
	vectorLabeled =[];
	matrixLabels =[];
	data = struct('X',[], 'L',[] ,'K',[], 'Y',[]);



	if(length(classIds) == 2) %binary case
		vectorIds= 1:n;
		
		tmpLabeledObjects = labeledObjects; % Create a temporary set of labeled objects
		tmpLabeledObjects(labeledObjects(:,2) == 1, 2) = 1;  % Positive label
		tmpLabeledObjects(labeledObjects(:,2) ~= 2, 2) =-1;  % Negative label

		vectorLabeled = labeledObjects(:,1);
		matrixLabels  = zeros(n,1);
		matrixLabels(vectorLabeled) = tmpLabeledObjects(:,2);

		% Reorder the matrices based on the labeled objects
		data.L   = [L(intersect(vectorLabeled, vectorIds), :); L(setdiff(vectorIds, vectorLabeled), :)];
		data.K   = [GramMatrix(intersect(vectorLabeled, vectorIds), :); GramMatrix(setdiff(vectorIds, vectorLabeled), :)];
		data.Y   = [matrixLabels(intersect(vectorLabeled, vectorIds), :); matrixLabels(setdiff(vectorIds, vectorLabeled), :)];
		data.X   = [dataSet(intersect(vectorLabeled, vectorIds), :); dataSet(setdiff(vectorIds, vectorLabeled), :)];
				
		classifier = lapsvmp(options,data)
	
%		Need to include the label extraction option... since we are just comparing runtime, we left this code out.

	else
		for cId = 1: length(classIds)
			tmp= classIds(cId);
			vectorIds= 1:n;
			
			tmpLabeledObjects = labeledObjects; % Create a temporary set of labeled objects
			tmpLabeledObjects(labeledObjects(:,2) == tmp, 2) = 1;  % Positive label
			tmpLabeledObjects(labeledObjects(:,2) ~= tmp, 2) =-1; % Negative label
	
			vectorLabeled = labeledObjects(:,1);
			matrixLabels  = zeros(n,1);
			matrixLabels(vectorLabeled) = tmpLabeledObjects(:,2);

			% Reorder the matrices based on the labeled objects
			data.L   = [L(intersect(vectorLabeled, vectorIds), :); L(setdiff(vectorIds, vectorLabeled), :)];
			data.K   = [GramMatrix(intersect(vectorLabeled, vectorIds), :); GramMatrix(setdiff(vectorIds, vectorLabeled), :)];
			data.Y   = [matrixLabels(intersect(vectorLabeled, vectorIds), :); matrixLabels(setdiff(vectorIds, vectorLabeled), :)];
			data.X   = [dataSet(intersect(vectorLabeled, vectorIds), :); dataSet(setdiff(vectorIds, vectorLabeled), :)];
				
			classifier = lapsvmp(options,data)
	
%			Need to include the label extraction option... since we are just comparing runtime, we left this code out.
		end
	end

	runtime.overall = toc(overallTime);
	runtime.process = toc(processTime);
end
